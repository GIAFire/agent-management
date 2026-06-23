package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.controller.AgentChatController;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.service.AgentChatService;
import com.zw.agent.service.AiAgentMessageService;
import com.zw.agent.service.AiAgentRunEventService;
import com.zw.agent.service.AiAgentRunService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 服务类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RequiredArgsConstructor
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AiAgentRunEventService agentRunEventService;
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunService agentRunService;
    @Override
    public Flux<ServerSentEvent<AgentChatController.AgentStreamResponse>> chatStream(AgentConfigDTO config, String tenantUserId, Long sessionId, String text,Long runId) {
        // 从上下文获取当前用户信息
        UserInfo userInfo = UserContext.get();
        // 用于累积大模型响应的文本内容
        StringBuilder assistantBuffer = new StringBuilder();
        // 用于记录事件序列号，保证递增
        AtomicInteger eventSeq = new AtomicInteger(0);
        Flux<ServerSentEvent<AgentChatController.AgentStreamResponse>> serverSentEventFlux = agentRuntimeFactory
                .callStreamEvents(config, tenantUserId, sessionId, text)
                .doOnNext(runtimeEvent -> {
                    // 如果事件包含增量内容，则追加到缓冲区
                    if (runtimeEvent.delta() != null) {
                        assistantBuffer.append(runtimeEvent.delta());
                    }

                    // 保存运行事件到数据库
                    agentRunEventService.saveEvent(
                            userInfo.getTenantId(),
                            runId,
                            sessionId,
                            eventSeq.incrementAndGet(),
                            runtimeEvent
                    );
                })
                // 将运行时事件映射为SSE事件
                .map(runtimeEvent -> ServerSentEvent.<AgentChatController.AgentStreamResponse>builder()
                        // 设置SSE事件类型
                        .event(runtimeEvent.sseEvent())
                        // 设置SSE事件数据
                        .data(new AgentChatController.AgentStreamResponse(
                                runId,
                                runtimeEvent.eventType(),
                                runtimeEvent.delta(),
                                runtimeEvent.rawEvent()
                        ))
                        // 构建SSE事件对象
                        .build()
                )
                // 在流结束后追加完成事件告诉前端流结束了
                .concatWith(Mono.defer(() -> {
                    // 保存AI的完整回复消息
                    AiAgentMessageEntity assistantMessage = agentMessageService.saveAssistantMessage(
                            userInfo.getTenantId(),
                            sessionId,
                            runId,
                            assistantBuffer.toString()
                    );

                    // 标记运行记录为成功状态
                    agentRunService.markSuccess(runId, assistantMessage.getId());

                    // 返回完成事件的SSE消息
                    return Mono.just(ServerSentEvent.<AgentChatController.AgentStreamResponse>builder()
                            .event("done")
                            .data(new AgentChatController.AgentStreamResponse(
                                    runId,
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
                            runId,
                            "AGENT_RUN_FAILED",
                            e.getMessage()
                    );

                    // 返回错误事件的SSE消息
                    return Flux.just(ServerSentEvent.<AgentChatController.AgentStreamResponse>builder()
                            .event("error")
                            .data(new AgentChatController.AgentStreamResponse(
                                    runId,
                                    "ERROR",
                                    "当前智能体执行失败，请稍后重试",
                                    Map.of("errorCode", "AGENT_RUN_FAILED")
                            ))
                            .build());
                });
        return serverSentEventFlux;
    }
}
