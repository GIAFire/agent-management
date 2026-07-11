package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.AiToolInfoConfigEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.event.AgentEventType;
import com.zw.agent.event.AgentRuntimeEvent;
import com.zw.agent.mapper.AiToolCallLogMapper;
import com.zw.agent.service.AiSubagentTaskService;
import com.zw.agent.service.AiToolCallLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.service.AiToolInfoConfigService;
import com.zw.common.context.UserInfo;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultDataDeltaEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@RequiredArgsConstructor
@Service
public class AiToolCallLogServiceImpl extends ServiceImpl<AiToolCallLogMapper, AiToolCallLogEntity> implements AiToolCallLogService {

    private static final int MAX_TEXT_LENGTH = 65_000;

    private final AiToolInfoConfigService toolInfoConfigService;
    private final AiSubagentTaskService subagentTaskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleToolCallAuditEvent(
            String eventType,
            AgentRuntimeEvent runtimeEvent,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Map<String, AiToolCallLogEntity> toolAuditMap
    ) {
        if (AgentEventType.TOOL_CALL_START.getValue().equals(eventType)) {
            ToolCallStartEvent rawEvent = (ToolCallStartEvent) runtimeEvent.getRawEvent();

            AiToolCallLogEntity auditEntity = createAuditEntity(
                    rawEvent.getToolCallId(),
                    rawEvent.getToolCallName(),
                    rawEvent.getReplyId(),
                    config,
                    userInfo,
                    sessionId,
                    runId
            );
            toolAuditMap.put(rawEvent.getToolCallId(), auditEntity);
            return;
        }

        if (AgentEventType.TOOL_CALL_DELTA.getValue().equals(eventType)) {
            ToolCallDeltaEvent rawEvent = (ToolCallDeltaEvent) runtimeEvent.getRawEvent();
            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            append(auditEntity.getToolInputBuffer(), rawEvent.getDelta());
            return;
        }

        if (AgentEventType.TOOL_CALL_END.getValue().equals(eventType)) {
            ToolCallEndEvent rawEvent = (ToolCallEndEvent) runtimeEvent.getRawEvent();
            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            auditEntity.setToolInputJson(toJsonMap(bufferText(auditEntity.getToolInputBuffer())));
            return;
        }

        if (AgentEventType.TOOL_RESULT_START.getValue().equals(eventType)) {
            ToolResultStartEvent rawEvent = (ToolResultStartEvent) runtimeEvent.getRawEvent();
            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            if (auditEntity.getStartedAt() == null) {
                auditEntity.setStartedAt(LocalDateTime.now());
            }
            return;
        }

        if (AgentEventType.TOOL_RESULT_TEXT_DELTA.getValue().equals(eventType)) {
            ToolResultTextDeltaEvent rawEvent = (ToolResultTextDeltaEvent) runtimeEvent.getRawEvent();
            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            append(auditEntity.getToolOutputBuffer(), rawEvent.getDelta());
            return;
        }

        if (AgentEventType.TOOL_RESULT_DATA_DELTA.getValue().equals(eventType)) {
            ToolResultDataDeltaEvent rawEvent = (ToolResultDataDeltaEvent) runtimeEvent.getRawEvent();
            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            append(auditEntity.getToolOutputBuffer(), toJson(rawEvent.getData()));
            return;
        }

        if (AgentEventType.TOOL_RESULT_END.getValue().equals(eventType)) {
            ToolResultEndEvent rawEvent = (ToolResultEndEvent) runtimeEvent.getRawEvent();

            AiToolCallLogEntity auditEntity = ensureAuditEntity(rawEvent.getToolCallId(), rawEvent.getToolCallName(),
                    rawEvent.getReplyId(), config, userInfo, sessionId, runId, toolAuditMap);
            auditEntity.setEndedAt(LocalDateTime.now());
            auditEntity.setSuccessStatus(rawEvent.getState() == null ? null : rawEvent.getState().getValue());
            auditEntity.setToolInputJson(firstMap(
                    auditEntity.getToolInputJson(),
                    toJsonMap(bufferText(auditEntity.getToolInputBuffer()))
            ));
            auditEntity.setToolOutputJson(toJsonMap(bufferText(auditEntity.getToolOutputBuffer())));
            auditEntity.setDurationMs(durationMs(auditEntity.getStartedAt(), auditEntity.getEndedAt()));
            if ("denied".equalsIgnoreCase(auditEntity.getSuccessStatus())) {
                auditEntity.setPermissionBehavior("DENY");
            }

            if (isMainAgentEvent(runtimeEvent) && isDelegationTool(auditEntity.getToolName())) {
                subagentTaskService.recordDelegationTask(auditEntity, config, userInfo, sessionId, runId);
            }
        }
    }

    private AiToolCallLogEntity ensureAuditEntity(
            String toolCallId,
            String toolName,
            String replyId,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId,
            Map<String, AiToolCallLogEntity> toolAuditMap
    ) {
        return toolAuditMap.computeIfAbsent(toolCallId, ignored -> createAuditEntity(
                toolCallId,
                toolName,
                replyId,
                config,
                userInfo,
                sessionId,
                runId
        ));
    }

    private AiToolCallLogEntity createAuditEntity(
            String toolCallId,
            String toolName,
            String replyId,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        AiToolCallLogEntity auditEntity = new AiToolCallLogEntity();
        auditEntity.setStartedAt(LocalDateTime.now());
        auditEntity.setToolName(toolName);
        auditEntity.setToolCallId(toolCallId);
        auditEntity.setReplyId(replyId);
        auditEntity.setTenantId(userInfo.getTenantId());
        auditEntity.setSessionId(sessionId);
        auditEntity.setRunId(runId);
        auditEntity.setAgentId(config.getAgentId());
        auditEntity.setAgentConfigId(config.getAgentConfigId());
        auditEntity.setUserId(userInfo.getUserId());
        auditEntity.setToolId(resolveToolId(userInfo.getTenantId(), toolName));
        auditEntity.setPermissionBehavior("ALLOW");
        auditEntity.setToolInputBuffer(new StringBuilder());
        auditEntity.setToolOutputBuffer(new StringBuilder());
        auditEntity.setCreatedBy(userInfo.getUserId());
        return auditEntity;
    }

    private Long resolveToolId(Long tenantId, String toolName) {
        if (tenantId == null || toolName == null || toolName.isBlank()) {
            return null;
        }
        AiToolInfoConfigEntity toolConfig = toolInfoConfigService.getOne(
                new LambdaQueryWrapper<AiToolInfoConfigEntity>()
                        .eq(AiToolInfoConfigEntity::getTenantId, tenantId)
                        .eq(AiToolInfoConfigEntity::getToolName, toolName)
                        .last("LIMIT 1"),
                false
        );
        return toolConfig == null ? null : toolConfig.getId();
    }

    private boolean isMainAgentEvent(AgentRuntimeEvent runtimeEvent) {
        AgentEvent rawEvent = runtimeEvent == null ? null : runtimeEvent.getRawEvent();
        String source = rawEvent == null ? null : rawEvent.getSource();
        return source == null || source.isBlank() || "main".equalsIgnoreCase(source.trim());
    }

    private boolean isDelegationTool(String toolName) {
        String normalized = normalize(toolName);
        return "agent_spawn".equals(normalized) || "agent_send".equals(normalized);
    }

    private void append(StringBuilder buffer, String value) {
        if (buffer != null && value != null && !value.isEmpty()) {
            buffer.append(value);
        }
    }

    private String bufferText(StringBuilder buffer) {
        return buffer == null ? null : buffer.toString();
    }

    private Long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            return null;
        }
        return Duration.between(startedAt, endedAt).toMillis();
    }

    private Map<String, Object> firstMap(Map<String, Object> first, Map<String, Object> second) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        return second == null || second.isEmpty() ? null : second;
    }

    private Map<String, Object> toJsonMap(String value) {
        String text = limit(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            Object parsed = objectMapper.readValue(text, Object.class);
            if (parsed instanceof Map<?, ?> parsedMap) {
                Map<String, Object> result = new LinkedHashMap<>();
                parsedMap.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
                return result;
            }
            if (parsed != null) {
                return Map.of("value", parsed);
            }
        } catch (Exception ignored) {
            // Non-JSON tool output is still useful audit data, store it under text.
        }
        return Map.of("text", text);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String limit(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
