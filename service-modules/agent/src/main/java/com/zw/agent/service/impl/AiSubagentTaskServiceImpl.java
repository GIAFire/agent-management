package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.entity.AiSubagentInstanceEntity;
import com.zw.agent.entity.AiSubagentTaskEntity;
import com.zw.agent.entity.AiToolCallLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiSubagentTaskMapper;
import com.zw.agent.service.AiSubagentInstanceService;
import com.zw.agent.service.AiSubagentService;
import com.zw.agent.service.AiSubagentTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RequiredArgsConstructor
@Service
public class AiSubagentTaskServiceImpl extends ServiceImpl<AiSubagentTaskMapper, AiSubagentTaskEntity> implements AiSubagentTaskService {

    private static final int MAX_TEXT_LENGTH = 65_000;

    private final AiSubagentService subagentService;
    private final AiSubagentInstanceService subagentInstanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void recordDelegationTask(
            AiToolCallLogEntity toolCallLog,
            AgentConfigDTO config,
            UserInfo userInfo,
            Long sessionId,
            Long runId
    ) {
        if (toolCallLog == null || !isDelegationTool(toolCallLog.getToolName())) {
            return;
        }

        Map<String, Object> input = toolCallLog.getToolInputJson();
        Map<String, Object> output = toolCallLog.getToolOutputJson();
        String outputText = mapText(output);

        String agentKey = firstText(
                value(input, "agent_key", "agentKey"),
                lineValue(output, "agent_key")
        );
        String runtimeSessionKey = lineValue(output, "session_id");
        String subagentExternalId = lineValue(output, "subagent_id");
        String taskId = lineValue(output, "task_id");
        String requestedAgent = firstText(
                value(input, "agent_id", "agentId", "subagent_id", "subagentId"),
                lineValue(output, "agent_id")
        );
        String label = value(input, "label");
        String taskInput = firstText(
                value(input, "task", "message", "input", "prompt", "content"),
                mapText(input),
                ""
        );
        Integer timeoutSeconds = parseInteger(value(input, "timeout_seconds", "timeoutSeconds"));

        AiSubagentEntity subagent = resolveSubagent(config, requestedAgent, label);
        AiSubagentInstanceEntity instance = resolveInstance(userInfo, config, sessionId, runId, agentKey, runtimeSessionKey, subagentExternalId);

        AiSubagentTaskEntity taskEntity = new AiSubagentTaskEntity();
        taskEntity.setTenantId(userInfo.getTenantId());
        taskEntity.setUserId(userInfo.getUserId());
        taskEntity.setParentAgentId(config.getAgentId());
        taskEntity.setParentAgentConfigId(config.getAgentConfigId());
        taskEntity.setParentSessionId(sessionId);
        taskEntity.setParentRunId(runId);
        taskEntity.setTaskId(taskId);
        taskEntity.setTaskMode(resolveTaskMode(taskId, timeoutSeconds));
        taskEntity.setTaskInput(defaultText(limit(taskInput)));
        taskEntity.setTaskResult(defaultText(limit(outputText)));
        taskEntity.setStatus(resolveStatus(toolCallLog.getSuccessStatus(), taskEntity.getTaskMode(), taskId, outputText));
        taskEntity.setTimeoutSeconds(timeoutSeconds);
        taskEntity.setStartedAt(toolCallLog.getStartedAt());
        taskEntity.setFinishedAt(finishedAt(taskEntity.getStatus(), toolCallLog.getEndedAt()));
        taskEntity.setDurationMs(durationMs(taskEntity.getStartedAt(), taskEntity.getFinishedAt()));
        taskEntity.setErrorMessage(errorMessage(taskEntity.getStatus(), outputText));
        taskEntity.setCreatedBy(userInfo.getUserId());

        if (instance != null) {
            taskEntity.setSubagentInstanceId(instance.getId());
            taskEntity.setSubagentId(instance.getSubagentId());
            taskEntity.setSubagentKey(instance.getSubagentKey());
        }
        if (subagent != null) {
            taskEntity.setSubagentId(firstNonNull(taskEntity.getSubagentId(), subagent.getId()));
            taskEntity.setSubagentKey(firstText(taskEntity.getSubagentKey(), subagent.getSubagentKey()));
        }
        if (taskEntity.getSubagentKey() == null) {
            taskEntity.setSubagentKey(firstText(requestedAgent, label));
        }

        save(taskEntity);
    }

    private boolean isDelegationTool(String toolName) {
        String normalized = normalize(toolName);
        return "agent_spawn".equals(normalized) || "agent_send".equals(normalized);
    }

    private AiSubagentEntity resolveSubagent(AgentConfigDTO config, String requestedAgent, String label) {
        if (config == null || config.getAgentId() == null) {
            return null;
        }
        List<AiSubagentEntity> subagents = subagentService.subAgentList(config.getAgentId());
        if (subagents == null || subagents.isEmpty()) {
            return null;
        }

        String requested = normalize(requestedAgent);
        String normalizedLabel = normalize(label);
        return subagents.stream()
                .filter(Objects::nonNull)
                .filter(subagent -> matchesSubagent(subagent, requested) || matchesSubagent(subagent, normalizedLabel))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesSubagent(AiSubagentEntity subagent, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.equals(normalize(subagent.getSubagentKey()))
                || value.equals(normalize(subagent.getSubagentName()))
                || value.equals(normalize(subagent.getDescription()));
    }

    private AiSubagentInstanceEntity resolveInstance(
            UserInfo userInfo,
            AgentConfigDTO config,
            Long sessionId,
            Long runId,
            String agentKey,
            String runtimeSessionKey,
            String subagentExternalId
    ) {
        if (!notBlank(agentKey) && !notBlank(runtimeSessionKey) && !notBlank(subagentExternalId)) {
            return null;
        }

        LambdaQueryWrapper<AiSubagentInstanceEntity> query = new LambdaQueryWrapper<>();
        query.eq(AiSubagentInstanceEntity::getTenantId, userInfo.getTenantId())
                .eq(AiSubagentInstanceEntity::getUserId, userInfo.getUserId())
                .eq(AiSubagentInstanceEntity::getParentAgentId, config.getAgentId())
                .eq(AiSubagentInstanceEntity::getParentAgentConfigId, config.getAgentConfigId())
                .eq(AiSubagentInstanceEntity::getParentSessionId, sessionId)
                .eq(AiSubagentInstanceEntity::getParentRunId, runId)
                .and(wrapper -> {
                    boolean hasCondition = false;
                    if (notBlank(agentKey)) {
                        wrapper.eq(AiSubagentInstanceEntity::getAgentKey, agentKey);
                        hasCondition = true;
                    }
                    if (notBlank(runtimeSessionKey)) {
                        if (hasCondition) {
                            wrapper.or();
                        }
                        wrapper.eq(AiSubagentInstanceEntity::getRuntimeSessionKey, runtimeSessionKey);
                        hasCondition = true;
                    }
                    if (notBlank(subagentExternalId)) {
                        if (hasCondition) {
                            wrapper.or();
                        }
                        wrapper.eq(AiSubagentInstanceEntity::getSubagentExternalId, subagentExternalId);
                    }
                })
                .last("LIMIT 1");
        return subagentInstanceService.getOne(query, false);
    }

    private String resolveTaskMode(String taskId, Integer timeoutSeconds) {
        if (notBlank(taskId) || (timeoutSeconds != null && timeoutSeconds == 0)) {
            return "BACKGROUND";
        }
        return "SYNC";
    }

    private String resolveStatus(String successStatus, String taskMode, String taskId, String output) {
        String normalizedStatus = normalize(successStatus);
        String normalizedOutput = normalize(output);
        if (normalizedOutput.contains("timeout")) {
            return "TIMEOUT";
        }
        if (normalizedOutput.contains("cancel")) {
            return "CANCELLED";
        }
        if (!"success".equals(normalizedStatus)) {
            return "FAILED";
        }
        if ("BACKGROUND".equals(taskMode) || notBlank(taskId)) {
            return "RUNNING";
        }
        return "COMPLETED";
    }

    private LocalDateTime finishedAt(String status, LocalDateTime endedAt) {
        if ("RUNNING".equals(status)) {
            return null;
        }
        return endedAt;
    }

    private Long durationMs(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, finishedAt).toMillis();
    }

    private String errorMessage(String status, String output) {
        if ("FAILED".equals(status) || "TIMEOUT".equals(status) || "CANCELLED".equals(status)) {
            return limit(output);
        }
        return null;
    }

    private String lineValue(Map<String, Object> output, String key) {
        String directValue = value(output, key, snakeToCamel(key));
        if (directValue != null) {
            return directValue;
        }
        return lineValue(mapText(output), key);
    }

    private String lineValue(String output, String key) {
        if (output == null || output.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*:\\s*(.*?)\\s*$");
        Matcher matcher = pattern.matcher(output);
        return matcher.find() ? blankToNull(matcher.group(1)) : null;
    }

    private String value(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return blankToNull(String.valueOf(value));
            }
        }
        return null;
    }

    private String mapText(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        String text = value(map, "text", "output", "result", "content", "message", "value");
        if (text != null) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ignored) {
            return String.valueOf(map);
        }
    }

    private String snakeToCamel(String value) {
        if (value == null || !value.contains("_")) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return builder.toString();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = blankToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String limit(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }
}
