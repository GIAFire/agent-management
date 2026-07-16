package com.zw.agent.controller;

import com.zw.agent.entity.AiAgentMessageLogEntity;
import com.zw.agent.entity.AiAgentRunLogEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.service.*;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/chat")
public class AgentChatController {

    private final AiAgentService agentService;
    private final AiAgentSessionService agentSessionService;
    private final AiAgentMessageLogService agentMessageService;
    private final AiAgentRunLogService agentRunService;
    private final AgentChatService agentChatService;


    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(@RequestBody AgentChatRequest request) {
        Long requestStartNs = System.nanoTime();
        UserInfo userInfo = UserContext.get();

        AgentConfigDTO agentConfig = agentService.getAgentConfigById(request.getAgentId());

        AiAgentSessionEntity session = requireOwnedSession(userInfo, request.getAgentId(), request.getSessionId());

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

        return agentChatService.chatStream(agentConfig,userInfo, session.getId(), request.getContent(),run.getId());
    }

    @PostMapping(value = "/userConfirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirm(@RequestBody AgentInterventionRequest request) {
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO config = agentService.getAgentConfigById(request.getAgentId());
        requireOwnedSession(userInfo, request.getAgentId(), request.getSessionId());
        return agentChatService.userConfirmStream(config, userInfo, request.getSessionId(), request);
    }

    @PostMapping(value = "/externalExecution", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> externalExecution(@RequestBody AgentInterventionRequest request) {
        UserInfo userInfo = UserContext.get();
        AgentConfigDTO config = agentService.getAgentConfigById(request.getAgentId());
        requireOwnedSession(userInfo, request.getAgentId(), request.getSessionId());
        return agentChatService.externalExecutionStream(config, userInfo, request.getSessionId(), request);
    }

    private AiAgentSessionEntity requireOwnedSession(UserInfo userInfo, Long agentId, Long sessionId) {
        AiAgentSessionEntity session = agentSessionService.getOwnedSession(userInfo, agentId, sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Agent session not found");
        }
        return session;
    }
}
