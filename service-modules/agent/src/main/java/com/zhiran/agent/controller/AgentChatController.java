package com.zhiran.agent.controller;

import com.zhiran.agent.entity.AiAgentMessageEntity;
import com.zhiran.agent.entity.AiAgentRunLogEntity;
import com.zhiran.agent.entity.AiAgentSessionEntity;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.entity.message.AgentChatRequest;
import com.zhiran.agent.entity.message.AgentInterventionRequest;
import com.zhiran.agent.event.AgentStreamResponse;
import com.zhiran.agent.service.*;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.context.UserInfo;
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
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunLogService agentRunService;
    private final AgentChatService agentChatService;


    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(@RequestBody AgentChatRequest request) {
        Long requestStartNs = System.nanoTime();
        UserInfo userInfo = UserContext.get();

        AiAgentSessionEntity session = requireOwnedSession(userInfo, request.getAgentId(), request.getSessionId());
        AgentConfigDTO agentConfig = agentService.getAgentConfigById(
                request.getAgentId(),
                session.getAgentConfigId(),
                userInfo
        );

        AiAgentMessageEntity userMessage = agentMessageService.saveUserMessage(
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
        agentMessageService.bindRunId(userMessage.getId(), run.getId());

        log.warn("Controller 里同步 DB 初始化耗时, runId={}, initCostMs={}",
                run.getId(),
                (System.nanoTime() - requestStartNs) / 1_000_000
        );

        return agentChatService.chatStream(agentConfig,userInfo, session.getId(), request.getContent(),run.getId());
    }

    @PostMapping(value = "/userConfirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirm(@RequestBody AgentInterventionRequest request) {
        UserInfo userInfo = UserContext.get();
        AiAgentSessionEntity session = requireOwnedSession(
                userInfo,
                request.getAgentId(),
                request.getSessionId()
        );
        AgentConfigDTO config = agentService.getAgentConfigById(
                request.getAgentId(),
                session.getAgentConfigId(),
                userInfo
        );
        return agentChatService.userConfirmStream(config, userInfo, request.getSessionId(), request);
    }

    @PostMapping(value = "/externalExecution", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> externalExecution(@RequestBody AgentInterventionRequest request) {
        UserInfo userInfo = UserContext.get();
        AiAgentSessionEntity session = requireOwnedSession(
                userInfo,
                request.getAgentId(),
                request.getSessionId()
        );
        AgentConfigDTO config = agentService.getAgentConfigById(
                request.getAgentId(),
                session.getAgentConfigId(),
                userInfo
        );
        return agentChatService.externalExecutionStream(config, userInfo, request.getSessionId(), request);
    }

    private AiAgentSessionEntity requireOwnedSession(UserInfo userInfo, Long agentId, Long sessionId) {
        AiAgentSessionEntity session = agentSessionService.getOwnedSession(userInfo, agentId, sessionId);
        if (session == null) {
            throw new IllegalArgumentException("当前会话不存在,请重新建立一次对话");
        }
        return session;
    }
}
