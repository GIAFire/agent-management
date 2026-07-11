package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zw.agent.factory.agentFactory.entity.AgentRuntimeStream;
import com.zw.agent.runtime.AgentRuntimeKeys;
import com.zw.agent.service.*;
import com.zw.agent.service.plan.PlanRuntimeEventTracker;
import com.zw.common.context.UserInfo;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.AgentState;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
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
    private final AiAgentRunEventLogService agentRunEventService;
    private final AiAgentMessageLogService agentMessageService;
    private final AiAgentRunLogService agentRunService;
    private final AiToolCallLogService toolCallAuditService;
    private final AiAgentStateLogService agentStateLogService;
    private final AiAgentStateOpLogService agentStateOpLogService;
    private final AiAgentPlanRuntimeService agentPlanRuntimeService;

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(AgentConfigDTO config, UserInfo userInfo, Long sessionId, String text, Long runId, Long requestStartNs, Long requestStartMs) {

        AgentRuntimeStream agentRuntimeStream = agentRuntimeFactory.callStreamEvents(config, userInfo, sessionId, runId, text);
        return streamRuntimeEvents(
                agentRuntimeStream.getAgent(),
                agentRuntimeStream.getRuntimeContext(),
                agentRuntimeStream.getRuntimeEvents(),
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
        AgentRuntimeStream agentRuntimeStream = agentRuntimeFactory
                .continueWithConfirmResults(config,
                        userInfo,
                        sessionId,
                        request.getRunId(),
                        toConfirmResults(request.getConfirmResults()));
        return streamRuntimeEvents(
                agentRuntimeStream.getAgent(),
                agentRuntimeStream.getRuntimeContext(),
                agentRuntimeStream.getRuntimeEvents(),
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
        AgentRuntimeStream agentRuntimeStream = agentRuntimeFactory.continueWithExternalExecutionResults(
                config,
                userInfo,
                sessionId,
                request.getRunId(),
                toToolResults(request.getToolResults())
        );
        return streamRuntimeEvents(
                agentRuntimeStream.getAgent(),
                agentRuntimeStream.getRuntimeContext(),
                agentRuntimeStream.getRuntimeEvents(),
                config,
                userInfo,
                sessionId,
                runId,
                requestStartNs,
                requestStartMs
        );
    }

    private Flux<ServerSentEvent<AgentStreamResponse>> streamRuntimeEvents(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            Flux<AgentRuntimeEvent> runtimeEvents,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Long requestStartNs,
            Long requestStartMs
    ) {
        StringBuilder assistantBuffer = new StringBuilder();
        AtomicReference<Integer> usageToken = new AtomicReference<>(0);
        AtomicReference<Double> usageTime = new AtomicReference<>(0.0);
        AtomicReference<String> waitingEventType = new AtomicReference<>();
        Map<String, AiToolCallLogEntity> toolAuditMap = new ConcurrentHashMap<>();
        AtomicLong streamSubscribeNs = new AtomicLong();
        AtomicBoolean firstEventLogged = new AtomicBoolean(false);
        AtomicLong seq = new AtomicLong(0);
        Long now = System.currentTimeMillis();
        PlanRuntimeEventTracker planEventTracker = agentPlanRuntimeService.newTracker();

        Set<String> loggedEventTypes = ConcurrentHashMap.newKeySet();

        AtomicBoolean stateLoadOpLogged = new AtomicBoolean(false);
        Flux<ServerSentEvent<AgentStreamResponse>> serverSentEventFlux = runtimeEvents
                .doOnSubscribe(subscription -> {
                    long nowNs = System.nanoTime();
                    streamSubscribeNs.set(nowNs);

                    log.warn("从接口入口到doOnSubscribe当前点总耗时, runId={}, costFromRequestStartMs={}",
                            runId,
                            (nowNs - requestStartNs) / 1_000_000
                    );
                })
                .doOnNext(runtimeEvent -> {
                    if (firstEventLogged.compareAndSet(false, true)) {
                        log.warn("从接口入口到doOnNext当前点总耗时, runId={}, costFromRequestStartMs={}, costFromSubscribeMs={}",
                                runId,
                                (System.nanoTime() - requestStartNs) / 1_000_000,
                                (System.nanoTime() - streamSubscribeNs.get()) / 1_000_000
                        );
                    }
                    if (stateLoadOpLogged.compareAndSet(false, true)) {
                        Mono.fromRunnable(() ->
                                        agentStateOpLogService.recordLoad(
                                                agent,
                                                userInfo,
                                                config,
                                                runtimeContext,
                                                sessionId,
                                                runId
                                        )
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe(
                                        null,
                                        ex -> log.warn("Record AgentState LOAD op failed, runId={}", runId, ex)
                                );
                    }
                    String eventType = runtimeEvent.getEventType();
                    if (!isSubAgentRuntimeEvent(runtimeEvent)
                            && eventType.equals(AgentEventType.TEXT_BLOCK_DELTA.getValue())
                            && runtimeEvent.getDelta() != null) {
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

                    toolCallAuditService.handleToolCallAuditEvent(eventType, runtimeEvent, config, userInfo, sessionId, runId, toolAuditMap);

                    if (isWaitingEvent(eventType)) {
                        waitingEventType.set(eventType);
                    }
                    loggedEventTypes.add(eventType);
                })
                .map(runtimeEvent -> {
                    // Plan 事件快照随原始事件一起下发，前端无需额外轮询计划和任务表。
                    Map<String, Object> planPayload = agentPlanRuntimeService.handleRuntimeEvent(
                            agent,
                            runtimeContext,
                            runtimeEvent,
                            config,
                            userInfo,
                            sessionId,
                            runId,
                            planEventTracker
                    );
                    return ServerSentEvent.<AgentStreamResponse>builder()
                        .event(toSseEventName(runtimeEvent))
                        .data(toStreamResponse(
                                runId,
                                runtimeEvent,
                                seq.incrementAndGet(),
                                mergePayload(toPayload(runtimeEvent), planPayload)
                        ))
                        .build();
                })
                .concatWith(Mono.defer(() -> {
                    long doneNs = System.nanoTime();
                    log.warn("服务端准备返回最终done事件的时间, runId={}, totalCostMs={}, streamCostMs={}",
                            runId,
                            (doneNs - requestStartNs) / 1_000_000,
                            (doneNs - streamSubscribeNs.get()) / 1_000_000
                    );

                    List<AiToolCallLogEntity> audits = new ArrayList<>(toolAuditMap.values());
                    toolAuditMap.clear();

                    Mono.fromRunnable(() -> {
                                agentMessageService.saveAssistantMessage(
                                        userInfo,
                                        sessionId,
                                        runId,
                                        assistantBuffer.toString(),
                                        config.getAgentName(),
                                        usageToken.get(),
                                        usageTime.get()
                                );
                                agentStateLogService.updateState(
                                        agent,
                                        runtimeContext,
                                        config,
                                        userInfo,
                                        sessionId,
                                        runId
                                );

                                if (waitingEventType.get() != null) {
                                    agentRunService.markWaiting(runId, waitingRunStatus(waitingEventType.get()));
                                }

                                if (!audits.isEmpty()) {
                                    toolCallAuditService.saveBatch(audits);
                                }
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    ex -> log.warn("保存state信息失败, runId={}", runId, ex)
                            );

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
                .onErrorResume(e -> {
                    long errorNs = System.nanoTime();

                    log.warn("Agent stream before send error, runId={}, totalCostMs={}, streamCostMs={}",
                            runId,
                            (errorNs - requestStartNs) / 1_000_000,
                            (errorNs - streamSubscribeNs.get()) / 1_000_000,
                            e
                    );

                    log.warn("Agent stream failed, runId={}", runId, e);

                    Mono.fromRunnable(() -> {
                                        agentRunService.markFailed(
                                                runId,
                                                "FAILED",
                                                e.getMessage()
                                        );
                                        agentStateOpLogService.recordSaveFailed(
                                                userInfo,
                                                sessionId,
                                                runId,
                                                e.getMessage()
                                        );
                                    })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(null,ex -> log.error("Mark agent run failed failed, runId={}", runId, ex));

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

                    log.warn("Agent stream finally, runId={}, signalType={}, totalCostMs={}, streamCostMs={}",
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
                                            userInfo,
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

    private boolean isSubAgentRuntimeEvent(AgentRuntimeEvent runtimeEvent) {
        if (runtimeEvent == null || runtimeEvent.getRawEvent() == null) {
            return false;
        }
        if (runtimeEvent.getRawEvent() instanceof SubagentExposedEvent) {
            return true;
        }

        String sourcePath = normalizeSourcePath(runtimeEvent.getRawEvent().getSource());
        return isSubAgentSource(sourcePath);
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
        if (rawEvent instanceof SubagentExposedEvent subagentExposedEvent) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("subagentId", subagentExposedEvent.getSubagentId());
            payload.put("agentId", subagentExposedEvent.getAgentId());
            payload.put("sessionId", subagentExposedEvent.getSessionId());
            payload.put("label", subagentExposedEvent.getLabel());
            return payload;
        }

        return null;
    }

    private AgentStreamResponse toStreamResponse(
            Long runId,
            AgentRuntimeEvent runtimeEvent,
            Long seq,
            Object payload
    ) {
        AgentStreamResponse response = new AgentStreamResponse(
                runId,
                runtimeEvent.getEventType(),
                runtimeEvent.getDelta(),
                seq,
                payload
        );

        AgentEvent rawEvent = runtimeEvent.getRawEvent();
        String sourcePath = rawEvent == null ? null : normalizeSourcePath(rawEvent.getSource());
        String subAgentName = resolveSubAgentName(sourcePath, rawEvent);
        if (!isSubAgentSource(sourcePath)) {
            sourcePath = null;
        }

        response.setSourcePath(sourcePath);
        response.setSubAgentName(subAgentName);
        response.setSubAgentInstanceId(metadataLong(rawEvent, "subAgentInstanceId", "subagentInstanceId",
                "sub_agent_instance_id", "subagent_instance_id", "instanceId"));
        response.setSubAgentTaskId(metadataLong(rawEvent, "subAgentTaskId", "subagentTaskId",
                "sub_agent_task_id", "subagent_task_id", "taskId"));
        return response;
    }

    /**
     * 合并普通事件 payload 和 Plan 快照 payload。
     * 原有结构继续保留，Plan 数据统一放在 planEvent 下，方便前端增量消费。
     */
    private Object mergePayload(Object payload, Map<String, Object> planPayload) {
        if (planPayload == null || planPayload.isEmpty()) {
            return payload;
        }

        // 保留原有 HITL payload，再追加 planEvent，避免影响用户确认弹窗的数据结构。
        Map<String, Object> merged = new LinkedHashMap<>();
        if (payload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> {
                if (key != null) {
                    merged.put(String.valueOf(key), value);
                }
            });
        } else if (payload != null) {
            merged.put("value", payload);
        }
        merged.put("planEvent", planPayload);
        return merged;
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


    private String toSseEventName(AgentRuntimeEvent runtimeEvent) {
        String eventType = runtimeEvent == null ? null : runtimeEvent.getEventType();
        if (AgentEventType.SUBAGENT_EXPOSED.getValue().equals(eventType)) {
            return "subagent_exposed";
        }

        String eventName = toBaseSseEventName(eventType);
        if (isSubAgentRuntimeEvent(runtimeEvent)) {
            return "subagent_" + eventName;
        }
        return eventName;
    }

    private String toBaseSseEventName(String eventType) {
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

    private String normalizeSourcePath(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return source.trim();
    }

    private boolean isSubAgentSource(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return false;
        }
        String normalized = sourcePath.trim();
        return !"main".equalsIgnoreCase(normalized);
    }

    private String resolveSubAgentName(String sourcePath, AgentEvent rawEvent) {
        if (rawEvent instanceof SubagentExposedEvent subagentExposedEvent) {
            return firstText(subagentExposedEvent.getLabel(), subagentExposedEvent.getSubagentId());
        }
        if (!isSubAgentSource(sourcePath)) {
            return null;
        }

        String normalized = sourcePath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < normalized.length() - 1) {
            return normalized.substring(lastSlash + 1);
        }
        return normalized;
    }

    private Long metadataLong(AgentEvent rawEvent, String... keys) {
        if (rawEvent == null || rawEvent.getMetadata() == null || rawEvent.getMetadata().isEmpty()) {
            return null;
        }

        for (String key : keys) {
            Object value = rawEvent.getMetadata().get(key);
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
