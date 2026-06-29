package com.zw.agent.controller;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentChatRequest;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.runtime.AgentFullConfigService;
import com.zw.agent.runtime.AgentRuntimeFactory;
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
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunService agentRunService;
    private final AgentChatService agentChatService;


    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(@RequestBody AgentChatRequest request) {
        // 统计耗时
        Long requestStartNs = System.nanoTime();
        Long requestStartMs = System.currentTimeMillis();
        // 从上下文获取当前用户信息
        UserInfo userInfo = UserContext.get();

        // 加载智能体的已发布配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(
                userInfo.getTenantId(),
                request.getAgentId()
        );

        // 获取或创建智能体会话
        AiAgentSessionEntity session = agentSessionService.getOrCreateSession(
                userInfo,
                request.getAgentId(),
                config.getAgentConfigId(),
                request.getSessionId()
        );

        // 保存用户发送的消息
        AiAgentMessageEntity userMessage = agentMessageService.saveUserMessage(
                userInfo,
                session.getId(),
                request.getContent()
        );

        // 创建运行记录，状态为运行中
        AiAgentRunEntity run = agentRunService.createRunningRun(
                userInfo,
                request.getAgentId(),
                config.getAgentConfigId(),
                session.getId(),
                userMessage.getId()
        );

        log.info("Controller 里同步 DB 初始化耗时, runId={}, initCostMs={}",
                run.getId(),
                (System.nanoTime() - requestStartNs) / 1_000_000
        );

        return agentChatService.chatStream(config,userInfo, session.getId(), request.getContent(),run.getId(),requestStartNs,
                requestStartMs);
    }

    @PostMapping(value = "/userConfirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirm(@RequestBody AgentInterventionRequest request) {
        // 统计耗时
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
        // 统计耗时
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
        // 获取运行时配置
        AgentConfigDTO config = agentFullConfigService.loadPublishedConfig(userInfo.getTenantId(), request.getAgentId());
        String tenantUserId = userInfo.getTenantId().toString()+"-"+userInfo.getUserId().toString();

        return agentRuntimeFactory
                .call(config, tenantUserId, request.getSessionId(), request.getContent());
    }

}
