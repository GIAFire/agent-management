package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.entity.message.AgentInterventionRequest;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.event.AgentStreamResponse;
import com.zw.agent.factory.agentFactory.AgentRuntimeFactory;
import com.zw.agent.factory.agentFactory.entity.AgentRuntimeStream;
import com.zw.agent.runtime.message.RunMessageTracker;
import com.zw.agent.runtime.message.RuntimeEventEnvelope;
import com.zw.agent.runtime.message.RuntimeMessageDraft;
import com.zw.agent.service.*;
import com.zw.agent.service.plan.PlanRuntimeEventTracker;
import com.zw.common.context.UserContext;
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
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

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
    private final AiAgentMessageService agentMessageService;
    private final AiAgentRunLogService agentRunService;
    private final AiToolCallLogService toolCallAuditService;
    private final AiAgentStateLogService agentStateLogService;
    private final AiAgentStateOpLogService agentStateOpLogService;
    private final AiAgentPlanRuntimeService agentPlanRuntimeService;

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> chatStream(AgentConfigDTO config, UserInfo userInfo, Long sessionId, String text, Long runId) {

        AgentRuntimeStream agentRuntimeStream = agentRuntimeFactory.callStreamEvents(config, userInfo, sessionId, runId, text);
        return streamRuntimeEvents(
                agentRuntimeStream.getAgent(),
                agentRuntimeStream.getRuntimeContext(),
                agentRuntimeStream.getRuntimeEvents(),
                config,
                userInfo,
                sessionId,
                runId
        );
    }

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> userConfirmStream(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            AgentInterventionRequest request
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
                runId
        );
    }

    @Override
    public Flux<ServerSentEvent<AgentStreamResponse>> externalExecutionStream(
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            AgentInterventionRequest request
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
                runId
        );
    }

    private Flux<ServerSentEvent<AgentStreamResponse>> streamRuntimeEvents(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            Flux<AgentRuntimeEvent> runtimeEvents,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        AtomicReference<Integer> usageToken = new AtomicReference<>(0);
        AtomicReference<Double> usageTime = new AtomicReference<>(0.0);
        AtomicReference<String> waitingEventType = new AtomicReference<>();
        AtomicReference<Throwable> streamError = new AtomicReference<>();
        Map<String, AiToolCallLogEntity> toolAuditMap = new ConcurrentHashMap<>();
        AtomicLong sseSeq = new AtomicLong(0);
        AtomicBoolean terminalEventSeen = new AtomicBoolean(false);
        AtomicBoolean finalizationStarted = new AtomicBoolean(false);
        PlanRuntimeEventTracker planEventTracker = agentPlanRuntimeService.newTracker();
        RunMessageTracker messageTracker = new RunMessageTracker(config.getAgentName());

        Set<String> loggedEventTypes = ConcurrentHashMap.newKeySet();

        AtomicBoolean stateLoadOpLogged = new AtomicBoolean(false);
        return runtimeEvents
                .map(runtimeEvent -> UserContext.callAs(userInfo, () -> {
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
                    return new RuntimeEventEnvelope(runtimeEvent, planPayload);
                }))
                .doOnNext(envelope -> UserContext.runAs(userInfo, () -> {
                    AgentRuntimeEvent runtimeEvent = envelope.runtimeEvent();
                    if (stateLoadOpLogged.compareAndSet(false, true)) {
                        recordStateLoadAsync(agent, runtimeContext, config, userInfo, sessionId, runId);
                    }
                    String eventType = runtimeEvent.getEventType();

                    if (eventType.equals(AgentEventType.MODEL_CALL_END.getValue())) {
                        ModelCallEndEvent modelCallEndEvent = (ModelCallEndEvent) runtimeEvent.getRawEvent();
                        ChatUsage usage = modelCallEndEvent.getUsage();
                        if (usage != null) {
                            usageToken.set(usage.getTotalTokens());
                            usageTime.set(usage.getTime());
                        }
                    }
                    if (!isSubAgentRuntimeEvent(runtimeEvent) && isTerminalEvent(eventType)) {
                        terminalEventSeen.set(true);
                    }

                    toolCallAuditService.handleToolCallAuditEvent(eventType, runtimeEvent, config, userInfo, sessionId, runId, toolAuditMap);

                    if (isWaitingEvent(eventType)) {
                        waitingEventType.set(eventType);
                    }
                    messageTracker.accept(runtimeEvent, envelope.planPayload());
                    loggedEventTypes.add(eventType);
                }))
                .map(envelope -> {
                    AgentRuntimeEvent runtimeEvent = envelope.runtimeEvent();
                    return ServerSentEvent.<AgentStreamResponse>builder()
                        .event(toSseEventName(runtimeEvent))
                        .data(toStreamResponse(
                                runId,
                                runtimeEvent,
                                sseSeq.incrementAndGet(),
                                mergePayload(toPayload(runtimeEvent), envelope.planPayload())
                        ))
                        .build();
                })
                .doOnError(streamError::set)
                .onErrorResume(e -> {
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
                    if (!finalizationStarted.compareAndSet(false, true)) {
                        return;
                    }

                    List<AiToolCallLogEntity> audits = new ArrayList<>(toolAuditMap.values());
                    toolAuditMap.clear();

                    Throwable error = streamError.get();
                    String waitingType = waitingEventType.get();
                    messageTracker.finishOpenMessages(signalType, error, waitingType);
                    List<RuntimeMessageDraft> messageDrafts = messageTracker.snapshot();
                    String runtimeEventTypes = eventTypes(loggedEventTypes);

                    Mono.fromRunnable(() -> UserContext.runAs(userInfo, () ->
                                    finalizeRuntime(
                                            agent,
                                            runtimeContext,
                                            config,
                                            userInfo,
                                            sessionId,
                                            runId,
                                            messageDrafts,
                                            usageToken.get(),
                                            usageTime.get(),
                                            audits,
                                            runtimeEventTypes,
                                            terminalEventSeen.get(),
                                            waitingType,
                                            error,
                                            signalType
                                    )
                            ))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    ex -> log.error("Agent运行收尾失败, runId={}", runId, ex)
                            );
                });
    }

    private void recordStateLoadAsync(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        Mono.fromRunnable(() -> UserContext.runAs(userInfo, () ->
                        agentStateOpLogService.recordLoad(
                                agent,
                                userInfo,
                                config,
                                runtimeContext,
                                sessionId,
                                runId
                        )
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        ex -> log.warn("Record AgentState LOAD op failed, runId={}", runId, ex)
                );
    }

    private void finalizeRuntime(
            HarnessAgent agent,
            RuntimeContext runtimeContext,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            List<RuntimeMessageDraft> messageDrafts,
            Integer usageToken,
            Double usageTime,
            List<AiToolCallLogEntity> audits,
            String runtimeEventTypes,
            boolean terminalEventSeen,
            String waitingEventType,
            Throwable error,
            SignalType signalType
    ) {
        if (audits != null && !audits.isEmpty()) {
            toolCallAuditService.saveBatch(audits);
        }

        AiAgentMessageEntity outputMessage = agentMessageService.saveRunMessages(
                userInfo,
                sessionId,
                runId,
                messageDrafts,
                usageToken,
                usageTime
        );

        if (error == null && !SignalType.CANCEL.equals(signalType)) {
            try {
                agentStateLogService.updateState(
                        agent,
                        runtimeContext,
                        config,
                        userInfo,
                        sessionId,
                        runId
                );
            } catch (Exception ex) {
                log.warn("保存 AgentState 失败, runId={}", runId, ex);
                agentStateOpLogService.recordSaveFailed(userInfo, sessionId, runId, safeErrorMessage(ex));
            }
        }

        if (error != null) {
            agentRunService.markFailed(runId, "FAILED", safeErrorMessage(error));
            agentStateOpLogService.recordSaveFailed(userInfo, sessionId, runId, safeErrorMessage(error));
        } else if (waitingEventType != null) {
            agentRunService.markWaiting(runId, waitingRunStatus(waitingEventType));
        } else if (SignalType.CANCEL.equals(signalType)) {
            agentRunService.markCancelled(runId);
        } else if (terminalEventSeen || SignalType.ON_COMPLETE.equals(signalType)) {
            agentRunService.markSuccess(runId, outputMessage == null ? null : outputMessage.getId());
        }

        agentRunEventService.saveEvent(userInfo, runId, sessionId, runtimeEventTypes);
    }

    private String eventTypes(Set<String> loggedEventTypes) {
        if (loggedEventTypes == null || loggedEventTypes.isEmpty()) {
            return "";
        }
        return loggedEventTypes.stream()
                .filter(Objects::nonNull)
                .sorted()
                .reduce((left, right) -> left + "-" + right)
                .orElse("");
    }

    private String safeErrorMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        message = message
                .replaceAll("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,}\\]]+", "$1***")
                .replaceAll("(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret)([\"'\\s:=]+)[^,\"'\\s}\\]]+", "$1$2***");
        return message.length() > 2_000 ? message.substring(0, 2_000) : message;
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

    private boolean isTerminalEvent(String eventType) {
        return AgentEventType.AGENT_END.getValue().equals(eventType)
                || AgentEventType.REQUEST_STOP.getValue().equals(eventType)
                || AgentEventType.EXCEED_MAX_ITERS.getValue().equals(eventType);
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
        if (rawEvent instanceof ToolCallStartEvent toolCallStartEvent) {
            return toolPayload(
                    toolCallStartEvent.getReplyId(),
                    toolCallStartEvent.getToolCallId(),
                    null
            );
        }
        if (rawEvent instanceof ToolCallDeltaEvent toolCallDeltaEvent) {
            return toolPayload(
                    toolCallDeltaEvent.getReplyId(),
                    toolCallDeltaEvent.getToolCallId(),
                    null
            );
        }
        if (rawEvent instanceof ToolCallEndEvent toolCallEndEvent) {
            return toolPayload(
                    toolCallEndEvent.getReplyId(),
                    toolCallEndEvent.getToolCallId(),
                    null
            );
        }
        if (rawEvent instanceof ToolResultStartEvent toolResultStartEvent) {
            return toolPayload(
                    toolResultStartEvent.getReplyId(),
                    toolResultStartEvent.getToolCallId(),
                    null
            );
        }
        if (rawEvent instanceof ToolResultTextDeltaEvent toolResultTextDeltaEvent) {
            return toolPayload(
                    toolResultTextDeltaEvent.getReplyId(),
                    toolResultTextDeltaEvent.getToolCallId(),
                    null
            );
        }
        if (rawEvent instanceof ToolResultDataDeltaEvent toolResultDataDeltaEvent) {
            Map<String, Object> payload = toolPayload(
                    toolResultDataDeltaEvent.getReplyId(),
                    toolResultDataDeltaEvent.getToolCallId(),
                    null
            );
            payload.put("data", toolResultDataDeltaEvent.getData());
            return payload;
        }
        if (rawEvent instanceof ToolResultEndEvent toolResultEndEvent) {
            return toolPayload(
                    toolResultEndEvent.getReplyId(),
                    toolResultEndEvent.getToolCallId(),
                    toolResultEndEvent.getState() == null ? null : toolResultEndEvent.getState().getValue()
            );
        }
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

    private Map<String, Object> toolPayload(String replyId, String toolCallId, String state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("replyId", replyId);
        payload.put("toolCallId", toolCallId);
        payload.put("state", state);
        return payload;
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

        if (AgentEventType.REQUEST_STOP.getValue().equals(eventType)) {
            return "request_stop";
        }

        if (AgentEventType.EXCEED_MAX_ITERS.getValue().equals(eventType)) {
            return "exceed_max_iters";
        }

        if (AgentEventType.MODEL_CALL_START.getValue().equals(eventType)) {
            return "model_call_start";
        }

        if (AgentEventType.MODEL_CALL_END.getValue().equals(eventType)) {
            return "model_call_end";
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
