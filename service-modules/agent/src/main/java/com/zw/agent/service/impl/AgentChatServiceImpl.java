package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.service.AgentChatService;
import com.zw.agent.service.AiAgentMessageService;
import com.zw.agent.service.AiAgentRunEventService;
import com.zw.agent.service.AiAgentRunService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.model.ChatUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(AgentConfigDTO config,UserInfo userInfo, Long sessionId, String text,Long runId) {
        // 从上下文获取当前用户信息
        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();
        // 用于累积大模型响应的文本内容
        StringBuilder assistantBuffer = new StringBuilder();
        AtomicReference<Integer> usageToken = new AtomicReference<>(0);
        AtomicReference<Double> usageTime = new AtomicReference<>(0.0);
        Set<String> loggedEventTypes = ConcurrentHashMap.newKeySet();
        AtomicLong seq = new AtomicLong(0);
        // 用于记录事件序列号，保证递增
        Flux<ServerSentEvent<AgentStreamResponse>> serverSentEventFlux = agentRuntimeFactory
                .callStreamEvents(config, tenantUserId, sessionId, text)
                .doOnNext(runtimeEvent -> {
                    // 如果事件包含增量内容，则追加到缓冲区
                    String eventType = runtimeEvent.getEventType();
                    if (eventType.equals(AgentEventType.TEXT_BLOCK_DELTA.getValue()) && runtimeEvent.getDelta() != null) {
                        assistantBuffer.append(runtimeEvent.getDelta());
                    }
                    if (eventType.equals(AgentEventType.MODEL_CALL_END.getValue())){
                        ModelCallEndEvent modelCallEndEvent = (ModelCallEndEvent) runtimeEvent.getRawEvent();
                        ChatUsage usage = modelCallEndEvent.getUsage();
                        usageToken.set(usage.getTotalTokens());
                        usageTime.set(usage.getTime());
                    }
                    // 保存事件类型
                    loggedEventTypes.add(eventType);
                })
                // 将运行时事件映射为SSE事件
                .map(runtimeEvent -> ServerSentEvent.<AgentStreamResponse>builder()
                        // 设置SSE事件类型
                        .event(toSseEventName(runtimeEvent.getRawEvent().getType().getValue()))
                        // 设置SSE事件数据
                        .data(new AgentStreamResponse(
                                runId,
                                runtimeEvent.getEventType(),
                                runtimeEvent.getDelta(),
                                seq.incrementAndGet()
                        ))
                        // 构建SSE事件对象
                        .build()
                )
                // 在流结束后追加完成事件告诉前端流结束了
                .concatWith(Mono.defer(() -> {
                    // 保存AI的完整回复消息
                    agentMessageService.saveAssistantMessage(
                            userInfo,
                            sessionId,
                            runId,
                            assistantBuffer.toString(),
                            config.getAgentName(),
                            usageToken.get(),
                            usageTime.get()
                    );
                    // 返回完成事件的SSE消息
                    return Mono.just(ServerSentEvent.<AgentStreamResponse>builder()
                            .event("done")
                            .data(new AgentStreamResponse(
                                    runId,
                                    "DONE",
                                    usageToken.get(),
                                    usageTime.get()
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
                    return Flux.just(ServerSentEvent.<AgentStreamResponse>builder()
                            .event("error")
                            .data(new AgentStreamResponse(
                                    runId,
                                    "ERROR",
                                    "当前智能体执行失败，请稍后重试",
                                    null
                            ))
                            .build());
                })
                .doFinally(signalType -> {
                    // 保存本次对话执行事件
                    String runtimeEvent = String.join("-", loggedEventTypes);
                    agentRunEventService.saveEvent(
                            userInfo.getUserId(),
                            userInfo.getTenantId(),
                            runId,
                            sessionId,
                            runtimeEvent
                    );
        });
        return serverSentEventFlux;
    }


    private String toSseEventName(String eventType) {
        if (eventType == null) {
            return "agent_event";
        }

        if (AgentEventType.AGENT_START.getValue().equals(eventType)) {
            return "agent_start";
        }

        if (AgentEventType.AGENT_END.getValue().equals(eventType)) {
            return "agent_end";
        }

        if (AgentEventType.TEXT_BLOCK_START.getValue().equals(eventType)) {
            return "message_start";
        }

        if (AgentEventType.TEXT_BLOCK_DELTA.getValue().equals(eventType)) {
            return "message_delta";
        }

        if (AgentEventType.TEXT_BLOCK_END.getValue().equals(eventType)) {
            return "message_end";
        }

        if (AgentEventType.THINKING_BLOCK_START.getValue().equals(eventType)) {
            return "thinking_start";
        }

        if (AgentEventType.THINKING_BLOCK_DELTA.getValue().equals(eventType)) {
            return "thinking_delta";
        }

        if (AgentEventType.THINKING_BLOCK_END.getValue().equals(eventType)) {
            return "thinking_end";
        }

        if (AgentEventType.TOOL_CALL_START.getValue().equals(eventType)) {
            return "tool_call_start";
        }

        if (AgentEventType.TOOL_CALL_END.getValue().equals(eventType)) {
            return "tool_call_end";
        }

        if (AgentEventType.TOOL_RESULT_START.getValue().equals(eventType)) {
            return "tool_result_start";
        }

        if (AgentEventType.TOOL_RESULT_TEXT_DELTA.getValue().equals(eventType)) {
            return "tool_result_text_delta";
        }

        if (AgentEventType.TOOL_RESULT_DATA_DELTA.getValue().equals(eventType)) {
            return "tool_result_data_delta";
        }

        if (AgentEventType.TOOL_RESULT_END.getValue().equals(eventType)) {
            return "tool_result_end";
        }

        // 工具调用、模型调用、异常、其他事件都可以先统一归类
        return "agent_event";
    }
}
