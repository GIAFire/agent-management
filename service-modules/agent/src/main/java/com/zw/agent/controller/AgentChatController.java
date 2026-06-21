package com.zw.agent.controller;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
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
        UserInfo userInfo = UserContext.get();

        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );

        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();

        AiAgentSessionEntity session = agentSessionService.getOrCreateSession(
                userInfo.getTenantId(),
                userInfo.getUserId(),
                request.getAgentId(),
                config.getAgentConfigId(),
                request.getSessionId()
        );

        AiAgentMessageEntity userMessage = agentMessageService.saveUserMessage(
                userInfo.getTenantId(),
                session.getId(),
                request.getContent()
        );

        AiAgentRunEntity run = agentRunService.createRunningRun(
                userInfo.getTenantId(),
                request.getAgentId(),
                config.getAgentConfigId(),
                session.getId(),
                userMessage.getId()
        );

        StringBuilder assistantBuffer = new StringBuilder();
        AtomicInteger eventSeq = new AtomicInteger(0);

        return agentRuntimeFactory
                .callStreamEvents(config, tenantUserId, request.getSessionId(), request.getContent())
                .doOnNext(runtimeEvent -> {
                    if (runtimeEvent.delta() != null) {
                        assistantBuffer.append(runtimeEvent.delta());
                    }

                    agentRunEventService.saveEvent(
                            userInfo.getTenantId(),
                            run.getId(),
                            session.getId(),
                            eventSeq.incrementAndGet(),
                            runtimeEvent
                    );
                })
                .map(runtimeEvent -> ServerSentEvent.<AgentStreamResponse>builder()
                        .event(runtimeEvent.sseEvent())
                        .data(new AgentStreamResponse(
                                run.getId(),
                                runtimeEvent.eventType(),
                                runtimeEvent.delta(),
                                runtimeEvent.rawEvent()
                        ))
                        .build()
                )
                .concatWith(Mono.defer(() -> {
                    AiAgentMessageEntity assistantMessage = agentMessageService.saveAssistantMessage(
                            userInfo.getTenantId(),
                            session.getId(),
                            run.getId(),
                            assistantBuffer.toString()
                    );

                    agentRunService.markSuccess(run.getId(), assistantMessage.getId());

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
                .onErrorResume(e -> {
                    agentRunService.markFailed(
                            run.getId(),
                            "AGENT_RUN_FAILED",
                            e.getMessage()
                    );

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
