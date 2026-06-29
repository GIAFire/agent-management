package com.zw.agent.service.impl;

import cn.hutool.core.date.DateTime;
import com.zw.agent.entity.AiToolCallAuditEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.runtime.AgentRuntimeFactory;
import com.zw.agent.service.*;
import com.zw.common.context.UserInfo;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.permission.PermissionRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
@Slf4j
@RequiredArgsConstructor
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentRuntimeFactory agentRuntimeFactory;
    private final AiAgentRunEventService agentRunEventService;
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunService agentRunService;
    private final AiToolCallAuditService toolCallAuditService;

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(AgentConfigDTO config,UserInfo userInfo, Long sessionId, String text,Long runId,Long requestStartNs, Long requestStartMs) {
        // 从上下文获取当前用户信息
        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();
        return streamRuntimeEvents(
                agentRuntimeFactory.callStreamEvents(config, tenantUserId, sessionId, text),
                config,
                userInfo,
                sessionId,
                runId,
                requestStartNs,
                requestStartMs
        );
    }

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirmStream(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            AgentInterventionRequest request,
            Long requestStartNs,
            Long requestStartMs
    ) {
        Long runId = requireRunId(request);
        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();
        return streamRuntimeEvents(
                agentRuntimeFactory.continueWithConfirmResults(
                        config,
                        tenantUserId,
                        sessionId,
                        toConfirmResults(request.getConfirmResults())
                ),
                config,
                userInfo,
                sessionId,
                runId,
                requestStartNs,
                requestStartMs
        );
    }

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> externalExecutionStream(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            AgentInterventionRequest request,
            Long requestStartNs,
            Long requestStartMs
    ) {
        Long runId = requireRunId(request);
        String tenantUserId = userInfo.getTenantId() + "-" + userInfo.getUserId();
        return streamRuntimeEvents(
                agentRuntimeFactory.continueWithExternalExecutionResults(
                        config,
                        tenantUserId,
                        sessionId,
                        toToolResults(request.getToolResults())
                ),
                config,
                userInfo,
                sessionId,
                runId,
                requestStartNs,
                requestStartMs
        );
    }

    private Flux<ServerSentEvent<AgentStreamResponse>> streamRuntimeEvents(
            Flux<AgentRuntimeEvent> runtimeEvents,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Long requestStartNs,
            Long requestStartMs
    ) {
        // 用于累积大模型响应的文本内容
        StringBuilder assistantBuffer = new StringBuilder();
        AtomicReference<Integer> usageToken = new AtomicReference<>(0);
        AtomicReference<Double> usageTime = new AtomicReference<>(0.0);
        AtomicReference<String> waitingEventType = new AtomicReference<>();
        Map<String, AiToolCallAuditEntity> toolAuditMap = new ConcurrentHashMap<>();
        // 统计耗时
        AtomicLong streamSubscribeNs = new AtomicLong();
        AtomicBoolean firstEventLogged = new AtomicBoolean(false);
        // 获取当前时间
        Long now = System.currentTimeMillis();

        Set<String> loggedEventTypes = ConcurrentHashMap.newKeySet();
        // 用于记录事件序列号，保证递增
        AtomicLong seq = new AtomicLong(0);
        Flux<ServerSentEvent<AgentStreamResponse>> serverSentEventFlux = runtimeEvents
                .doOnSubscribe(subscription -> {
                    long nowNs = System.nanoTime();
                    streamSubscribeNs.set(nowNs);

                    log.error("从接口入口到doOnSubscribe当前点总耗时, runId={}, costFromRequestStartMs={}",
                            runId,
                            (nowNs - requestStartNs) / 1_000_000
                    );
                })
                .doOnNext(runtimeEvent -> {
                    if (firstEventLogged.compareAndSet(false, true)) {
                        log.error("从接口入口到doOnNext当前点总耗时, runId={}, costFromRequestStartMs={}, costFromSubscribeMs={}",
                                runId,
                                (System.nanoTime() - requestStartNs) / 1_000_000,
                                (System.nanoTime() - streamSubscribeNs.get()) / 1_000_000
                        );
                    }
                    // 如果事件包含增量内容，则追加到缓冲区
                    String eventType = runtimeEvent.getEventType();
                    if (eventType.equals(AgentEventType.TEXT_BLOCK_DELTA.getValue()) && runtimeEvent.getDelta() != null) {
                        assistantBuffer.append(runtimeEvent.getDelta());
                    }
                    if (eventType.equals(AgentEventType.MODEL_CALL_END.getValue())) {
                        ModelCallEndEvent modelCallEndEvent = (ModelCallEndEvent) runtimeEvent.getRawEvent();
                        ChatUsage usage = modelCallEndEvent.getUsage();
                        if (usage != null) {
                            usageToken.set(usage.getTotalTokens());
                            usageTime.set(usage.getTime());
                        }
                    }

                    // 记录工具调用事件
                    toolCallAuditService.handleToolCallAuditEvent(eventType, runtimeEvent, config, userInfo, sessionId, runId, toolAuditMap);

                    if (isWaitingEvent(eventType)) {
                        waitingEventType.set(eventType);
                    }
                    // 保存事件类型
                    loggedEventTypes.add(eventType);
                })
                // 将运行时事件映射为SSE事件
                .map(runtimeEvent -> ServerSentEvent.<AgentStreamResponse>builder()
                        // 设置SSE事件类型
                        .event(toSseEventName(runtimeEvent.getEventType()))
                        // 设置SSE事件数据
                        .data(new AgentStreamResponse(
                                runId,
                                runtimeEvent.getEventType(),
                                runtimeEvent.getDelta(),
                                seq.incrementAndGet(),
                                toPayload(runtimeEvent)
                        ))
                        // 构建SSE事件对象
                        .build()
                )
                // 在流结束后追加完成事件告诉前端流结束了
                .concatWith(Mono.defer(() -> {
                    long doneNs = System.nanoTime();
                    log.error("服务端准备返回最终done事件的时间, runId={}, totalCostMs={}, streamCostMs={}",
                            runId,
                            (doneNs - requestStartNs) / 1_000_000,
                            (doneNs - streamSubscribeNs.get()) / 1_000_000
                    );

                    String assistantText = assistantBuffer.toString();
                    Integer totalTokens = usageToken.get();
                    Double totalTime = usageTime.get();
                    String waitingType = waitingEventType.get();
                    List<AiToolCallAuditEntity> audits = new ArrayList<>(toolAuditMap.values());
                    toolAuditMap.clear();

                    Mono.fromRunnable(() -> {
                                agentMessageService.saveAssistantMessage(
                                        userInfo,
                                        sessionId,
                                        runId,
                                        assistantText,
                                        config.getAgentName(),
                                        totalTokens,
                                        totalTime
                                );

                                if (waitingType != null) {
                                    agentRunService.markWaiting(runId, waitingRunStatus(waitingType));
                                }

                                if (!audits.isEmpty()) {
                                    toolCallAuditService.saveBatch(audits);
                                }
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    ex -> log.warn("Save stream final data failed, runId={}", runId, ex)
                            );

                    return Mono.just(ServerSentEvent.<AgentStreamResponse>builder()
                            .event("done")
                            .data(new AgentStreamResponse(
                                    runId,
                                    "DONE",
                                    totalTokens,
                                    totalTime
                            ))
                            .build());
                }))
                // 异常处理：当流式调用失败时
                .onErrorResume(e -> {
                    long errorNs = System.nanoTime();

                    log.error("Agent stream before send error, runId={}, totalCostMs={}, streamCostMs={}",
                            runId,
                            (errorNs - requestStartNs) / 1_000_000,
                            (errorNs - streamSubscribeNs.get()) / 1_000_000,
                            e
                    );

                    log.error("Agent stream failed, runId={}", runId, e);

                    Mono.fromRunnable(() ->
                                    agentRunService.markFailed(
                                            runId,
                                            "FAILED",
                                            e.getMessage()
                                    )
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    ex -> log.warn("Mark agent run failed failed, runId={}", runId, ex)
                            );

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
                    long finishNs = System.nanoTime();

                    log.error("Agent stream finally, runId={}, signalType={}, totalCostMs={}, streamCostMs={}",
                            runId,
                            signalType,
                            (finishNs - requestStartNs) / 1_000_000,
                            streamSubscribeNs.get() > 0
                                    ? (finishNs - streamSubscribeNs.get()) / 1_000_000
                                    : -1
                    );

                    String runtimeEventTypes = String.join("-", loggedEventTypes);

                    Mono.fromRunnable(() ->
                                    agentRunEventService.saveEvent(
                                            userInfo.getUserId(),
                                            userInfo.getTenantId(),
                                            runId,
                                            sessionId,
                                            runtimeEventTypes
                                    )
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    ex -> log.warn("Save agent run event failed, runId={}", runId, ex)
                            );
                });
        return serverSentEventFlux;
    }

    private Long requireRunId(AgentInterventionRequest request) {
        if (request == null || request.getRunId() == null) {
            throw new IllegalArgumentException("runId不能为空");
        }
        return request.getRunId();
    }

    private boolean isWaitingEvent(String eventType) {
        return AgentEventType.REQUIRE_USER_CONFIRM.getValue().equals(eventType)
                || AgentEventType.REQUIRE_EXTERNAL_EXECUTION.getValue().equals(eventType);
    }

    private String waitingRunStatus(String eventType) {
        if (AgentEventType.REQUIRE_EXTERNAL_EXECUTION.getValue().equals(eventType)) {
            return "WAITING_EXTERNAL_EXECUTION";
        }
        return "WAITING_USER_CONFIRM";
    }

    private List<ConfirmResult> toConfirmResults(List<AgentInterventionRequest.ToolConfirmRequest> confirmRequests) {
        if (confirmRequests == null || confirmRequests.isEmpty()) {
            throw new IllegalArgumentException("confirmResults不能为空");
        }
        return confirmRequests.stream()
                .filter(Objects::nonNull)
                .map(confirmRequest -> new ConfirmResult(
                        Boolean.TRUE.equals(confirmRequest.getConfirmed()),
                        toToolUseBlock(confirmRequest.getToolCall())
                ))
                .toList();
    }

    private ToolUseBlock toToolUseBlock(AgentInterventionRequest.ToolCallRequest toolCallRequest) {
        if (toolCallRequest == null) {
            throw new IllegalArgumentException("toolCall不能为空");
        }
        return new ToolUseBlock(
                toolCallRequest.getId(),
                toolCallRequest.getName(),
                safeMap(toolCallRequest.getInput()),
                toolCallRequest.getContent(),
                safeMap(toolCallRequest.getMetadata())
        );
    }

    private List<ToolResultBlock> toToolResults(List<AgentInterventionRequest.ToolExecutionResultRequest> toolResultRequests) {
        if (toolResultRequests == null || toolResultRequests.isEmpty()) {
            throw new IllegalArgumentException("toolResults不能为空");
        }
        return toolResultRequests.stream()
                .filter(Objects::nonNull)
                .map(toolResultRequest -> ToolResultBlock.builder()
                        .id(toolResultRequest.getToolCallId())
                        .name(toolResultRequest.getToolName())
                        .output(TextBlock.builder().text(toolResultRequest.getText()).build())
                        .metadata(safeMap(toolResultRequest.getMetadata()))
                        .state(resolveToolResultState(toolResultRequest))
                        .build())
                .toList();
    }

    private ToolResultState resolveToolResultState(AgentInterventionRequest.ToolExecutionResultRequest toolResultRequest) {
        String state = toolResultRequest.getState();
        if (state != null && !state.isBlank()) {
            try {
                return ToolResultState.valueOf(state.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 兼容前端传入 success/error 这类非枚举值。
            }
        }
        return Boolean.FALSE.equals(toolResultRequest.getSuccess())
                ? ToolResultState.ERROR
                : ToolResultState.SUCCESS;
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private Object toPayload(AgentRuntimeEvent runtimeEvent) {
        if (runtimeEvent == null) {
            return null;
        }

        AgentEvent rawEvent = runtimeEvent.getRawEvent();
        if (rawEvent instanceof RequireUserConfirmEvent requireUserConfirmEvent) {
            return interventionPayload(requireUserConfirmEvent.getReplyId(), requireUserConfirmEvent.getToolCalls());
        }
        if (rawEvent instanceof RequireExternalExecutionEvent requireExternalExecutionEvent) {
            return interventionPayload(requireExternalExecutionEvent.getReplyId(), requireExternalExecutionEvent.getToolCalls());
        }
        if (rawEvent instanceof UserConfirmResultEvent userConfirmResultEvent) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("replyId", userConfirmResultEvent.getReplyId());
            payload.put("confirmResults", userConfirmResultEvent.getConfirmResults().stream()
                    .map(this::confirmResultPayload)
                    .toList());
            return payload;
        }
        if (rawEvent instanceof ExternalExecutionResultEvent externalExecutionResultEvent) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("replyId", externalExecutionResultEvent.getReplyId());
            payload.put("toolResults", externalExecutionResultEvent.getToolResults().stream()
                    .map(this::toolResultPayload)
                    .toList());
            return payload;
        }

        return null;
    }

    private Map<String, Object> interventionPayload(String replyId, List<ToolUseBlock> toolCalls) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("replyId", replyId);
        payload.put("toolCalls", toolCalls == null ? List.of() : toolCalls.stream()
                .map(this::toolUsePayload)
                .toList());
        return payload;
    }

    private Map<String, Object> toolUsePayload(ToolUseBlock toolUseBlock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (toolUseBlock == null) {
            return payload;
        }
        payload.put("id", toolUseBlock.getId());
        payload.put("name", toolUseBlock.getName());
        payload.put("input", toolUseBlock.getInput());
        payload.put("content", toolUseBlock.getContent());
        payload.put("metadata", toolUseBlock.getMetadata());
        payload.put("state", toolUseBlock.getState() == null ? null : toolUseBlock.getState().getValue());
        return payload;
    }

    private Map<String, Object> confirmResultPayload(ConfirmResult confirmResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("confirmed", confirmResult.isConfirmed());
        payload.put("toolCall", toolUsePayload(confirmResult.getToolCall()));
        payload.put("rules", confirmResult.getRules() == null ? List.of() : confirmResult.getRules().stream()
                .map(this::permissionRulePayload)
                .toList());
        return payload;
    }

    private Map<String, Object> permissionRulePayload(PermissionRule rule) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (rule == null) {
            return payload;
        }
        payload.put("toolName", rule.toolName());
        payload.put("ruleContent", rule.ruleContent());
        payload.put("behavior", rule.behavior() == null ? null : rule.behavior().getValue());
        payload.put("source", rule.source());
        return payload;
    }

    private Map<String, Object> toolResultPayload(ToolResultBlock toolResultBlock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (toolResultBlock == null) {
            return payload;
        }
        payload.put("id", toolResultBlock.getId());
        payload.put("name", toolResultBlock.getName());
        payload.put("metadata", toolResultBlock.getMetadata());
        payload.put("state", toolResultBlock.getState() == null ? null : toolResultBlock.getState().getValue());
        payload.put("output", toolResultBlock.getOutput() == null ? List.of() : toolResultBlock.getOutput().stream()
                .map(this::contentBlockPayload)
                .toList());
        return payload;
    }

    private Map<String, Object> contentBlockPayload(ContentBlock contentBlock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (contentBlock instanceof TextBlock textBlock) {
            payload.put("type", "text");
            payload.put("text", textBlock.getText());
            return payload;
        }
        payload.put("type", contentBlock == null ? "unknown" : contentBlock.getClass().getSimpleName());
        payload.put("text", contentBlock == null ? "" : contentBlock.toString());
        return payload;
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

        if (AgentEventType.TOOL_CALL_DELTA.getValue().equals(eventType)) {
            return "tool_call_delta";
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

        if (AgentEventType.REQUIRE_USER_CONFIRM.getValue().equals(eventType)) {
            return "require_user_confirm";
        }

        if (AgentEventType.REQUIRE_EXTERNAL_EXECUTION.getValue().equals(eventType)) {
            return "require_external_execution";
        }

        if (AgentEventType.USER_CONFIRM_RESULT.getValue().equals(eventType)) {
            return "user_confirm_result";
        }

        if (AgentEventType.EXTERNAL_EXECUTION_RESULT.getValue().equals(eventType)) {
            return "external_execution_result";
        }

        // 工具调用、模型调用、异常、其他事件都可以先统一归类
        return "agent_event";
    }
}
