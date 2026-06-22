package com.zw.agent.controller;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.runtime.AgentFullConfigService;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.AiAgentMessageService;
import com.zw.agent.service.AiAgentRunEventService;
import com.zw.agent.service.AiAgentRunService;
import com.zw.agent.service.AiAgentSessionService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentFullConfigService agentFullConfigService;
    private final AiAgentSessionService agentSessionService;
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunService agentRunService;
    private final AiAgentRunEventService agentRunEventService;


    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(@RequestBody AgentChatRequest request) {
        // 从上下文获取当前用户信息
        UserInfo userInfo = UserContext.get();

        // 加载智能体的已发布配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );

        // 构建租户用户ID，格式为"租户ID-用户ID"
        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();

        // 获取或创建智能体会话
        AiAgentSessionEntity session = agentSessionService.getOrCreateSession(
                userInfo.getTenantId(),
                userInfo.getUserId(),
                request.getAgentId(),
                config.getAgentConfigId(),
                request.getSessionId()
        );

        // 保存用户发送的消息
        AiAgentMessageEntity userMessage = agentMessageService.saveUserMessage(
                userInfo.getTenantId(),
                session.getId(),
                request.getContent()
        );

        // 创建运行记录，状态为运行中
        AiAgentRunEntity run = agentRunService.createRunningRun(
                userInfo.getTenantId(),
                request.getAgentId(),
                config.getAgentConfigId(),
                session.getId(),
                userMessage.getId()
        );

        // 用于累积大模型响应的文本内容
        StringBuilder assistantBuffer = new StringBuilder();
        // 用于记录事件序列号，保证递增
        AtomicInteger eventSeq = new AtomicInteger(0);

        Flux<AgentRuntimeEvent> agentRuntimeEventFlux = agentRuntimeFactory
                .callStreamEvents(config, tenantUserId, request.getSessionId(), request.getContent());

        // 调用智能体运行时工厂，获取流式事件
        return agentRuntimeFactory
                .callStreamEvents(config, tenantUserId, request.getSessionId(), request.getContent())
                // 对每个到达的运行时事件进行处理
                .doOnNext(runtimeEvent -> {
                    // 如果事件包含增量内容，则追加到缓冲区
                    if (runtimeEvent.delta() != null) {
                        assistantBuffer.append(runtimeEvent.delta());
                    }

                    // 保存运行事件到数据库
                    agentRunEventService.saveEvent(
                            userInfo.getTenantId(),
                            run.getId(),
                            session.getId(),
                            eventSeq.incrementAndGet(),
                            runtimeEvent
                    );
                })
                // 将运行时事件映射为SSE事件
                .map(runtimeEvent -> ServerSentEvent.<AgentStreamResponse>builder()
                        // 设置SSE事件类型
                        .event(runtimeEvent.sseEvent())
                        // 设置SSE事件数据
                        .data(new AgentStreamResponse(
                                run.getId(),
                                runtimeEvent.eventType(),
                                runtimeEvent.delta(),
                                runtimeEvent.rawEvent()
                        ))
                        // 构建SSE事件对象
                        .build()
                )
                // 在流结束后追加完成事件
                .concatWith(Mono.defer(() -> {
                    // 保存助手的完整回复消息
                    AiAgentMessageEntity assistantMessage = agentMessageService.saveAssistantMessage(
                            userInfo.getTenantId(),
                            session.getId(),
                            run.getId(),
                            assistantBuffer.toString()
                    );

                    // 标记运行记录为成功状态
                    agentRunService.markSuccess(run.getId(), assistantMessage.getId());

                    // 返回完成事件的SSE消息
                    return Mono.just(ServerSentEvent.<AgentStreamResponse>builder()
                            .event("done")
                            .data(new AgentStreamResponse(
                                    run.getId(),
                                    "DONE",
                                    null,
                                    null
                            ))
                            .build());
                }))
                // 异常处理：当流式调用失败时
                .onErrorResume(e -> {
                    // 标记运行记录为失败状态
                    agentRunService.markFailed(
                            run.getId(),
                            "AGENT_RUN_FAILED",
                            e.getMessage()
                    );

                    // 返回错误事件的SSE消息
                    return Flux.just(ServerSentEvent.<AgentStreamResponse>builder()
                            .event("error")
                            .data(new AgentStreamResponse(
                                    run.getId(),
                                    "ERROR",
                                    "当前智能体执行失败，请稍后重试",
                                    Map.of("errorCode", "AGENT_RUN_FAILED")
                            ))
                            .build());
                });
    }

    @PostMapping("/chatBlock")
    public Mono<AgentChatResponse> chatBlock(@RequestBody AgentChatRequest request) {
        UserInfo userInfo = UserContext.get();
        // 获取运行时配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(userInfo.getTenantId(), request.getAgentId());
        String tenantUserId = userInfo.getTenantId().toString()+"-"+userInfo.getUserId().toString();

        return agentRuntimeFactory
                .call(config, tenantUserId, request.getSessionId(), request.getContent())
                .map(AgentChatResponse::new);
    }

    public record AgentChatResponse(String content) {}
    public record AgentStreamResponse(
            Long runId,
            String eventType,
            String delta,
            Object payload
    ) {}
}
