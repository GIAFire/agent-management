package com.zw.agent.runtime.message;

import com.zw.agent.event.AgentEventType;
import com.zw.agent.event.AgentRuntimeEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.ExternalExecutionResultEvent;
import io.agentscope.core.event.RequireExternalExecutionEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.RequestStopEvent;
import io.agentscope.core.event.SubagentExposedEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultDataDeltaEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.event.UserConfirmResultEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.permission.PermissionRule;
import reactor.core.publisher.SignalType;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RunMessageTracker {

    private static final int MAX_TOOL_RESULT_TEXT_LENGTH = 20_000;
    private static final int MAX_SAFE_TEXT_LENGTH = 65_000;
    private static final Set<String> PLAN_TOOL_NAMES = Set.of(
            "plan_enter",
            "plan_write",
            "plan_exit",
            "todo_write"
    );
    private static final Set<String> SUBAGENT_TOOL_NAMES = Set.of(
            "agent_spawn",
            "agent_send",
            "agent_wait",
            "agent_result"
    );
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "access_token",
            "refresh_token",
            "token",
            "api_key",
            "apikey",
            "secret",
            "password",
            "passwd",
            "credential"
    );

    private final String agentName;
    private final Map<String, RuntimeMessageDraft> messages = new LinkedHashMap<>();
    private final Map<String, String> activeTextKeys = new LinkedHashMap<>();
    private final Map<String, String> activeThinkingKeys = new LinkedHashMap<>();
    private final Map<String, String> toolCallMessageKeys = new LinkedHashMap<>();
    private final Map<String, String> toolResultMessageKeys = new LinkedHashMap<>();
    private final Set<String> persistedPlanSnapshots = new HashSet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private int textOrdinal;
    private int thinkingOrdinal;
    private int fallbackToolOrdinal;
    private int noticeOrdinal;

    public RunMessageTracker(String agentName) {
        this.agentName = agentName;
    }

    public void accept(AgentRuntimeEvent runtimeEvent, Map<String, Object> planPayload) {
        if (runtimeEvent == null) {
            return;
        }

        String eventType = runtimeEvent.getEventType();
        if (!isSubAgentRuntimeEvent(runtimeEvent)) {
            if (AgentEventType.THINKING_BLOCK_START.getValue().equals(eventType)) {
                handleThinkingStart(runtimeEvent);
            } else if (AgentEventType.THINKING_BLOCK_DELTA.getValue().equals(eventType)) {
                handleThinkingDelta(runtimeEvent);
            } else if (AgentEventType.THINKING_BLOCK_END.getValue().equals(eventType)) {
                handleThinkingEnd(runtimeEvent);
            } else if (AgentEventType.TEXT_BLOCK_START.getValue().equals(eventType)) {
                handleTextStart(runtimeEvent);
            } else if (AgentEventType.TEXT_BLOCK_DELTA.getValue().equals(eventType)) {
                handleTextDelta(runtimeEvent);
            } else if (AgentEventType.TEXT_BLOCK_END.getValue().equals(eventType)) {
                handleTextEnd(runtimeEvent);
            } else if (AgentEventType.TOOL_CALL_START.getValue().equals(eventType)) {
                handleToolCallStart(runtimeEvent);
            } else if (AgentEventType.TOOL_CALL_DELTA.getValue().equals(eventType)) {
                handleToolCallDelta(runtimeEvent);
            } else if (AgentEventType.TOOL_CALL_END.getValue().equals(eventType)) {
                handleToolCallEnd(runtimeEvent);
            } else if (AgentEventType.TOOL_RESULT_START.getValue().equals(eventType)) {
                handleToolResultStart(runtimeEvent);
            } else if (AgentEventType.TOOL_RESULT_TEXT_DELTA.getValue().equals(eventType)) {
                handleToolResultTextDelta(runtimeEvent);
            } else if (AgentEventType.TOOL_RESULT_DATA_DELTA.getValue().equals(eventType)) {
                handleToolResultDataDelta(runtimeEvent);
            } else if (AgentEventType.TOOL_RESULT_END.getValue().equals(eventType)) {
                handleToolResultEnd(runtimeEvent);
            } else if (AgentEventType.REQUIRE_USER_CONFIRM.getValue().equals(eventType)) {
                handleRequireUserConfirm(runtimeEvent);
            } else if (AgentEventType.USER_CONFIRM_RESULT.getValue().equals(eventType)) {
                handleUserConfirmResult(runtimeEvent);
            } else if (AgentEventType.REQUIRE_EXTERNAL_EXECUTION.getValue().equals(eventType)) {
                handleRequireExternalExecution(runtimeEvent);
            } else if (AgentEventType.EXTERNAL_EXECUTION_RESULT.getValue().equals(eventType)) {
                handleExternalExecutionResult(runtimeEvent);
            } else if (AgentEventType.EXCEED_MAX_ITERS.getValue().equals(eventType)) {
                addNotice(runtimeEvent, "Agent reached the maximum iteration limit and stopped.");
            } else if (AgentEventType.REQUEST_STOP.getValue().equals(eventType)
                    && runtimeEvent.getRawEvent() instanceof RequestStopEvent) {
                addNotice(runtimeEvent, "Agent requested to stop the current run.");
            }
        }

        handlePlanPayload(planPayload);
    }

    public void finishOpenMessages(SignalType signalType, Throwable error, String waitingEventType) {
        for (RuntimeMessageDraft draft : messages.values()) {
            if (!"STREAMING".equals(draft.getMessageStatus())) {
                continue;
            }
            if (error != null) {
                draft.fail("STREAM_ERROR", safeErrorMessage(error));
            } else if (SignalType.CANCEL.equals(signalType)) {
                draft.cancel();
            } else {
                draft.complete();
            }
        }

        if (error != null) {
            addErrorMessage(error);
        }
    }

    public List<RuntimeMessageDraft> snapshot() {
        return new ArrayList<>(messages.values());
    }

    private void handleThinkingStart(AgentRuntimeEvent runtimeEvent) {
        String replyId = messageId(runtimeEvent);
        String runtimeKey = replyId + ":THINKING:" + (++thinkingOrdinal);
        RuntimeMessageDraft draft = baseDraft(runtimeKey, "ASSISTANT", "ASSISTANT_THINKING", agentName, "MARKDOWN", runtimeEvent)
                .textBuffer(new StringBuilder())
                .build();
        messages.put(runtimeKey, draft);
        activeThinkingKeys.put(replyId, runtimeKey);
    }

    private void handleThinkingDelta(AgentRuntimeEvent runtimeEvent) {
        String replyId = messageId(runtimeEvent);
        String runtimeKey = activeThinkingKeys.get(replyId);
        if (runtimeKey == null) {
            handleThinkingStart(runtimeEvent);
            runtimeKey = activeThinkingKeys.get(replyId);
        }
        RuntimeMessageDraft draft = messages.get(runtimeKey);
        if (draft != null) {
            draft.append(runtimeEvent.getDelta());
        }
    }

    private void handleThinkingEnd(AgentRuntimeEvent runtimeEvent) {
        String runtimeKey = activeThinkingKeys.remove(messageId(runtimeEvent));
        complete(runtimeKey);
    }

    private void handleTextStart(AgentRuntimeEvent runtimeEvent) {
        String replyId = messageId(runtimeEvent);
        String runtimeKey = replyId + ":TEXT:" + (++textOrdinal);
        RuntimeMessageDraft draft = baseDraft(runtimeKey, "ASSISTANT", "ASSISTANT_TEXT", agentName, "MARKDOWN", runtimeEvent)
                .textBuffer(new StringBuilder())
                .build();
        messages.put(runtimeKey, draft);
        activeTextKeys.put(replyId, runtimeKey);
    }

    private void handleTextDelta(AgentRuntimeEvent runtimeEvent) {
        String replyId = messageId(runtimeEvent);
        String runtimeKey = activeTextKeys.get(replyId);
        if (runtimeKey == null) {
            handleTextStart(runtimeEvent);
            runtimeKey = activeTextKeys.get(replyId);
        }
        RuntimeMessageDraft draft = messages.get(runtimeKey);
        if (draft != null) {
            draft.append(runtimeEvent.getDelta());
        }
    }

    private void handleTextEnd(AgentRuntimeEvent runtimeEvent) {
        String runtimeKey = activeTextKeys.remove(messageId(runtimeEvent));
        complete(runtimeKey);
    }

    private void handleToolCallStart(AgentRuntimeEvent runtimeEvent) {
        ToolCallStartEvent event = (ToolCallStartEvent) runtimeEvent.getRawEvent();
        String toolCallId = toolCallId(event.getToolCallId(), runtimeEvent);
        String toolName = firstText(event.getToolCallName(), "tool");
        InvocationKind kind = classify(toolName);
        String runtimeKey = toolCallId + ":" + kind.callMessageType;

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("toolName", toolName);
        content.put("toolCallId", toolCallId);

        RuntimeMessageDraft draft = baseDraft(runtimeKey, "ASSISTANT", kind.callMessageType, toolName, "CARD", runtimeEvent)
                .textBuffer(new StringBuilder())
                .content(content)
                .refType(kind.refType)
                .refKey(toolCallId)
                .build();
        messages.put(runtimeKey, draft);
        toolCallMessageKeys.put(toolCallId, runtimeKey);
    }

    private void handleToolCallDelta(AgentRuntimeEvent runtimeEvent) {
        ToolCallDeltaEvent event = (ToolCallDeltaEvent) runtimeEvent.getRawEvent();
        String runtimeKey = toolCallMessageKeys.get(toolCallId(event.getToolCallId(), runtimeEvent));
        RuntimeMessageDraft draft = messages.get(runtimeKey);
        if (draft != null) {
            draft.append(runtimeEvent.getDelta());
        }
    }

    private void handleToolCallEnd(AgentRuntimeEvent runtimeEvent) {
        ToolCallEndEvent event = (ToolCallEndEvent) runtimeEvent.getRawEvent();
        String runtimeKey = toolCallMessageKeys.get(toolCallId(event.getToolCallId(), runtimeEvent));
        RuntimeMessageDraft draft = messages.get(runtimeKey);
        if (draft == null) {
            return;
        }

        Object safeArguments = redact(parseJsonOrText(draft.getTextContent()));
        draft.getContent().put("arguments", safeArguments);
        draft.setTextBuffer(new StringBuilder(toJsonOrString(safeArguments)));
        draft.complete();
    }

    private void handleToolResultStart(AgentRuntimeEvent runtimeEvent) {
        ToolResultStartEvent event = (ToolResultStartEvent) runtimeEvent.getRawEvent();
        String toolName = firstText(event.getToolCallName(), "tool");
        InvocationKind kind = classify(toolName);
        if (InvocationKind.PLAN.equals(kind)) {
            return;
        }

        String toolCallId = toolCallId(event.getToolCallId(), runtimeEvent);
        String runtimeKey = toolCallId + ":" + kind.resultMessageType;
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("toolName", toolName);
        content.put("toolCallId", toolCallId);

        RuntimeMessageDraft draft = baseDraft(runtimeKey, "TOOL", kind.resultMessageType, toolName, "CARD", runtimeEvent)
                .parentRuntimeKey(toolCallMessageKeys.get(toolCallId))
                .textBuffer(new StringBuilder())
                .content(content)
                .refType(kind.refType)
                .refKey(toolCallId)
                .build();
        messages.put(runtimeKey, draft);
        toolResultMessageKeys.put(toolCallId, runtimeKey);
    }

    private void handleToolResultTextDelta(AgentRuntimeEvent runtimeEvent) {
        ToolResultTextDeltaEvent event = (ToolResultTextDeltaEvent) runtimeEvent.getRawEvent();
        RuntimeMessageDraft draft = messages.get(toolResultMessageKeys.get(toolCallId(event.getToolCallId(), runtimeEvent)));
        if (draft != null) {
            appendWithLimit(draft, redactSensitiveText(runtimeEvent.getDelta()), MAX_TOOL_RESULT_TEXT_LENGTH);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleToolResultDataDelta(AgentRuntimeEvent runtimeEvent) {
        ToolResultDataDeltaEvent event = (ToolResultDataDeltaEvent) runtimeEvent.getRawEvent();
        RuntimeMessageDraft draft = messages.get(toolResultMessageKeys.get(toolCallId(event.getToolCallId(), runtimeEvent)));
        if (draft == null) {
            return;
        }
        List<Object> dataBlocks = (List<Object>) draft.getContent().computeIfAbsent("dataBlocks", ignored -> new ArrayList<>());
        dataBlocks.add(redact(event.getData()));
    }

    private void handleToolResultEnd(AgentRuntimeEvent runtimeEvent) {
        ToolResultEndEvent event = (ToolResultEndEvent) runtimeEvent.getRawEvent();
        String toolCallId = toolCallId(event.getToolCallId(), runtimeEvent);
        RuntimeMessageDraft draft = messages.get(toolResultMessageKeys.get(toolCallId));
        if (draft == null) {
            return;
        }
        draft.getContent().put("state", event.getState() == null ? null : event.getState().getValue());
        draft.getContent().put("resultText", draft.getTextContent());
        draft.complete();
    }

    private void handleRequireUserConfirm(AgentRuntimeEvent runtimeEvent) {
        RequireUserConfirmEvent event = (RequireUserConfirmEvent) runtimeEvent.getRawEvent();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("replyId", event.getReplyId());
        content.put("toolCalls", toolCallsPayload(event.getToolCalls()));
        addCompletedCard(runtimeEvent, "CONFIRM_REQUEST:" + messageId(runtimeEvent), "ASSISTANT",
                "CONFIRM_REQUEST", agentName, "Waiting for user confirmation", content);
    }

    private void handleUserConfirmResult(AgentRuntimeEvent runtimeEvent) {
        UserConfirmResultEvent event = (UserConfirmResultEvent) runtimeEvent.getRawEvent();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("replyId", event.getReplyId());
        content.put("confirmResults", event.getConfirmResults() == null ? List.of() : event.getConfirmResults().stream()
                .map(this::confirmResultPayload)
                .toList());
        addCompletedCard(runtimeEvent, "CONFIRM_RESULT:" + messageId(runtimeEvent), "USER",
                "CONFIRM_RESULT", "User", "User confirmation submitted", content);
    }

    private void handleRequireExternalExecution(AgentRuntimeEvent runtimeEvent) {
        RequireExternalExecutionEvent event = (RequireExternalExecutionEvent) runtimeEvent.getRawEvent();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("replyId", event.getReplyId());
        content.put("toolCalls", toolCallsPayload(event.getToolCalls()));
        addCompletedCard(runtimeEvent, "EXTERNAL_EXECUTION_REQUEST:" + messageId(runtimeEvent), "ASSISTANT",
                "EXTERNAL_EXECUTION_REQUEST", agentName, "Waiting for external execution", content);
    }

    private void handleExternalExecutionResult(AgentRuntimeEvent runtimeEvent) {
        ExternalExecutionResultEvent event = (ExternalExecutionResultEvent) runtimeEvent.getRawEvent();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("replyId", event.getReplyId());
        content.put("toolResults", event.getToolResults() == null ? List.of() : event.getToolResults().stream()
                .map(this::toolResultPayload)
                .toList());
        addCompletedCard(runtimeEvent, "EXTERNAL_EXECUTION_RESULT:" + messageId(runtimeEvent), "TOOL",
                "EXTERNAL_EXECUTION_RESULT", "External executor", "External execution result received", content);
    }

    private void handlePlanPayload(Map<String, Object> planPayload) {
        if (planPayload == null || planPayload.isEmpty() || !(planPayload.get("plan") instanceof Map<?, ?> planMap)) {
            return;
        }

        String type = Objects.toString(planPayload.get("type"), "PLAN_SNAPSHOT");
        String toolCallId = Objects.toString(planPayload.get("toolCallId"), "");
        Long planId = toLong(planMap.get("id"));
        String status = Objects.toString(planMap.get("status"), "");
        int taskCount = planPayload.get("tasks") instanceof List<?> tasks ? tasks.size() : 0;
        String dedupKey = planId + ":" + type + ":" + toolCallId + ":" + status + ":" + taskCount;
        if (!persistedPlanSnapshots.add(dedupKey)) {
            return;
        }

        String runtimeKey = "PLAN:" + dedupKey;
        String title = firstText(Objects.toString(planMap.get("title"), null), "Execution plan");
        Map<String, Object> content = new LinkedHashMap<>(planPayload);
        RuntimeMessageDraft draft = RuntimeMessageDraft.builder()
                .runtimeKey(runtimeKey)
                .role("ASSISTANT")
                .messageType("PLAN_SNAPSHOT")
                .messageStatus("COMPLETED")
                .senderName(agentName)
                .contentFormat("CARD")
                .textBuffer(new StringBuilder(title))
                .content(content)
                .refType("AGENT_PLAN")
                .refId(planId)
                .refKey(Objects.toString(planMap.get("planNo"), null))
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .durationMs(0L)
                .build();
        messages.put(runtimeKey, draft);
    }

    private void addNotice(AgentRuntimeEvent runtimeEvent, String text) {
        String runtimeKey = "NOTICE:" + (++noticeOrdinal) + ":" + messageId(runtimeEvent);
        RuntimeMessageDraft draft = RuntimeMessageDraft.builder()
                .runtimeKey(runtimeKey)
                .role("SYSTEM")
                .messageType("SYSTEM_NOTICE")
                .messageStatus("COMPLETED")
                .senderName("System")
                .contentFormat("TEXT")
                .textBuffer(new StringBuilder(text))
                .externalMessageId(messageId(runtimeEvent))
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .durationMs(0L)
                .build();
        messages.put(runtimeKey, draft);
    }

    private void addErrorMessage(Throwable error) {
        String runtimeKey = "ERROR:" + System.nanoTime();
        RuntimeMessageDraft draft = RuntimeMessageDraft.builder()
                .runtimeKey(runtimeKey)
                .role("SYSTEM")
                .messageType("ERROR")
                .messageStatus("FAILED")
                .senderName("System")
                .contentFormat("TEXT")
                .textBuffer(new StringBuilder("当前智能体执行失败，请稍后重试"))
                .errorCode("STREAM_ERROR")
                .errorMessage(safeErrorMessage(error))
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .durationMs(0L)
                .build();
        messages.put(runtimeKey, draft);
    }

    private void addCompletedCard(
            AgentRuntimeEvent runtimeEvent,
            String runtimeKey,
            String role,
            String messageType,
            String senderName,
            String text,
            Map<String, Object> content
    ) {
        RuntimeMessageDraft draft = RuntimeMessageDraft.builder()
                .runtimeKey(runtimeKey)
                .role(role)
                .messageType(messageType)
                .messageStatus("COMPLETED")
                .senderName(senderName)
                .contentFormat("CARD")
                .textBuffer(new StringBuilder(text))
                .content(content)
                .externalMessageId(messageId(runtimeEvent))
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .durationMs(0L)
                .build();
        messages.put(runtimeKey, draft);
    }

    private RuntimeMessageDraft.RuntimeMessageDraftBuilder baseDraft(
            String runtimeKey,
            String role,
            String messageType,
            String senderName,
            String contentFormat,
            AgentRuntimeEvent runtimeEvent
    ) {
        return RuntimeMessageDraft.builder()
                .runtimeKey(runtimeKey)
                .role(role)
                .messageType(messageType)
                .messageStatus("STREAMING")
                .senderName(senderName)
                .contentFormat(contentFormat)
                .externalMessageId(messageId(runtimeEvent))
                .startedAt(LocalDateTime.now());
    }

    private void complete(String runtimeKey) {
        RuntimeMessageDraft draft = messages.get(runtimeKey);
        if (draft != null) {
            draft.complete();
        }
    }

    private void appendWithLimit(RuntimeMessageDraft draft, String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int currentLength = draft.getTextBuffer() == null ? 0 : draft.getTextBuffer().length();
        int remaining = maxLength - currentLength;
        if (remaining <= 0) {
            draft.getContent().put("truncated", true);
            return;
        }
        if (value.length() > remaining) {
            draft.append(value.substring(0, remaining));
            draft.getContent().put("truncated", true);
        } else {
            draft.append(value);
        }
    }

    private Object parseJsonOrText(String text) {
        String safeText = limit(redactSensitiveText(text), MAX_SAFE_TEXT_LENGTH);
        if (safeText == null || safeText.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(safeText, Object.class);
        } catch (Exception ignored) {
            return Map.of("text", safeText);
        }
    }

    @SuppressWarnings("unchecked")
    private Object redact(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> redacted = new LinkedHashMap<>();
            source.forEach((key, mapValue) -> {
                String keyText = String.valueOf(key);
                if (isSensitiveKey(keyText)) {
                    redacted.put(keyText, "***");
                } else {
                    redacted.put(keyText, redact(mapValue));
                }
            });
            return redacted;
        }
        if (value instanceof List<?> source) {
            return source.stream().map(this::redact).toList();
        }
        if (value instanceof String text) {
            return limit(redactSensitiveText(text), MAX_SAFE_TEXT_LENGTH);
        }
        return value;
    }

    private String redactSensitiveText(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replaceAll("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,}\\]]+", "$1***")
                .replaceAll("(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret)([\"'\\s:=]+)[^,\"'\\s}\\]]+", "$1$2***");
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.endsWith("_token")
                || normalized.endsWith("_secret")
                || normalized.contains("password");
    }

    private List<Map<String, Object>> toolCallsPayload(List<ToolUseBlock> toolCalls) {
        if (toolCalls == null) {
            return List.of();
        }
        return toolCalls.stream()
                .filter(Objects::nonNull)
                .map(this::toolUsePayload)
                .toList();
    }

    private Map<String, Object> toolUsePayload(ToolUseBlock toolUseBlock) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", toolUseBlock.getId());
        payload.put("name", toolUseBlock.getName());
        payload.put("input", redact(toolUseBlock.getInput()));
        payload.put("content", redact(toolUseBlock.getContent()));
        payload.put("metadata", redact(toolUseBlock.getMetadata()));
        payload.put("state", toolUseBlock.getState() == null ? null : toolUseBlock.getState().getValue());
        return payload;
    }

    private Map<String, Object> confirmResultPayload(ConfirmResult confirmResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("confirmed", confirmResult.isConfirmed());
        payload.put("toolCall", confirmResult.getToolCall() == null ? null : toolUsePayload(confirmResult.getToolCall()));
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
        payload.put("metadata", redact(toolResultBlock.getMetadata()));
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
            payload.put("text", redactSensitiveText(textBlock.getText()));
            return payload;
        }
        payload.put("type", contentBlock == null ? "unknown" : contentBlock.getClass().getSimpleName());
        payload.put("text", contentBlock == null ? "" : limit(redactSensitiveText(contentBlock.toString()), MAX_SAFE_TEXT_LENGTH));
        return payload;
    }

    private InvocationKind classify(String toolName) {
        String normalized = normalizeToolName(toolName);
        if (PLAN_TOOL_NAMES.contains(normalized)) {
            return InvocationKind.PLAN;
        }
        if (SUBAGENT_TOOL_NAMES.contains(normalized)) {
            return InvocationKind.SUBAGENT;
        }
        if (normalized.contains("skill")) {
            return InvocationKind.SKILL;
        }
        return InvocationKind.TOOL;
    }

    private String toolCallId(String toolCallId, AgentRuntimeEvent runtimeEvent) {
        if (toolCallId != null && !toolCallId.isBlank()) {
            return toolCallId;
        }
        return messageId(runtimeEvent) + ":TOOL:" + (++fallbackToolOrdinal);
    }

    private String messageId(AgentRuntimeEvent runtimeEvent) {
        if (runtimeEvent == null) {
            return "unknown";
        }
        if (runtimeEvent.getReplyId() != null && !runtimeEvent.getReplyId().isBlank()) {
            return runtimeEvent.getReplyId();
        }
        AgentEvent rawEvent = runtimeEvent.getRawEvent();
        if (rawEvent != null && rawEvent.getId() != null && !rawEvent.getId().isBlank()) {
            return rawEvent.getId();
        }
        return "unknown";
    }

    private boolean isSubAgentRuntimeEvent(AgentRuntimeEvent runtimeEvent) {
        if (runtimeEvent == null || runtimeEvent.getRawEvent() == null) {
            return false;
        }
        if (runtimeEvent.getRawEvent() instanceof SubagentExposedEvent) {
            return true;
        }
        String source = runtimeEvent.getRawEvent().getSource();
        return source != null && !source.isBlank() && !"main".equalsIgnoreCase(source.trim());
    }

    private String normalizeToolName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private String toJsonOrString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String safeErrorMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        return limit(redactSensitiveText(error.getMessage()), 2_000);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private enum InvocationKind {
        TOOL("TOOL_CALL", "TOOL_RESULT", "TOOL_CALL_LOG"),
        SKILL("SKILL_CALL", "SKILL_RESULT", "SKILL_LOG"),
        SUBAGENT("SUBAGENT_CALL", "SUBAGENT_RESULT", "SUBAGENT_TASK"),
        PLAN("PLAN_OPERATION", "PLAN_SNAPSHOT", "AGENT_PLAN");

        private final String callMessageType;
        private final String resultMessageType;
        private final String refType;

        InvocationKind(String callMessageType, String resultMessageType, String refType) {
            this.callMessageType = callMessageType;
            this.resultMessageType = resultMessageType;
            this.refType = refType;
        }
    }
}
