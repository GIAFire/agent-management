package com.zw.agent.controller;

import com.zw.agent.entity.AiAgentMessageLogEntity;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.runtime.AgentFullConfigService;
import com.zw.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zw.agent.service.*;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AgentFullConfigService agentFullConfigService;
    private final AiAgentSessionService agentSessionService;
    private final AiAgentMessageLogService agentMessageService;
    private final AiAgentRunLogService agentRunService;
    private final AgentChatService agentChatService;


    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(@RequestBody AgentChatRequest request) {
        Long requestStartNs = System.nanoTime();
        Long requestStartMs = System.currentTimeMillis();
        UserInfo userInfo = UserContext.get();

        AgentConfigDTO agentConfig = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );

        AiAgentSessionEntity session = agentSessionService.getOrCreateSession(
                userInfo,
                request.getAgentId(),
                agentConfig.getAgentConfigId(),
                request.getSessionId()
        );

        AiAgentMessageLogEntity userMessage = agentMessageService.saveUserMessage(
                userInfo,
                session.getId(),
                request.getContent()
        );

        AiAgentRunLogEntity run = agentRunService.createRunningRun(
                userInfo,
                request.getAgentId(),
                agentConfig.getAgentConfigId(),
                session.getId(),
                userMessage.getId()
        );

        log.warn("Controller 里同步 DB 初始化耗时, runId={}, initCostMs={}",
                run.getId(),
                (System.nanoTime() - requestStartNs) / 1_000_000
        );

        return agentChatService.chatStream(agentConfig,userInfo, session.getId(), request.getContent(),run.getId(),requestStartNs,
                requestStartMs);
    }

    @PostMapping(value = "/userConfirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirm(@RequestBody AgentInterventionRequest request) {
        Long requestStartNs = System.nanoTime();
        Long requestStartMs = System.currentTimeMillis();
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );
        return agentChatService.userConfirmStream(config, userInfo, request.getSessionId(), request,requestStartNs,requestStartMs);
    }

    @PostMapping(value = "/externalExecution", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> externalExecution(@RequestBody AgentInterventionRequest request) {
        Long requestStartNs = System.nanoTime();
        Long requestStartMs = System.currentTimeMillis();
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );
        return agentChatService.externalExecutionStream(config, userInfo, request.getSessionId(), request,requestStartNs,
                requestStartMs);
    }

    @PostMapping("/chatBlock")
    public Mono<String> chatBlock(@RequestBody AgentChatRequest request) {
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(userInfo.getTenantId(), request.getAgentId());

        return agentRuntimeFactory
                .call(config, userInfo.getUserId(), request.getSessionId(), request.getContent());
    }

}
