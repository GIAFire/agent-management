package com.zw.agent.runtime.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.AiSkillLogEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.runtime.AgentCallContext;
import com.zw.agent.service.AiSkillInfoService;
import com.zw.agent.service.AiSkillLogService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Slf4j
public class SkillUsageLogMiddleware implements MiddlewareBase {

    private static final String SKILL_TOOL = "load_skill_through_path";
    private static final String MYSQL_SUFFIX = "_mysql";
    private static final int MAX_ERROR_LENGTH = 2000;

    private final AiSkillInfoService skillService;
    private final AiSkillLogService logService;
    private final AgentConfigDTO runtimeConfig;

    public SkillUsageLogMiddleware(
            AiSkillInfoService skillService,
            AiSkillLogService logService,
            AgentConfigDTO runtimeConfig
    ) {
        this.skillService = Objects.requireNonNull(skillService);
        this.logService = Objects.requireNonNull(logService);
        this.runtimeConfig = Objects.requireNonNull(runtimeConfig);
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext runtimeContext,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next
    ) {
        Map<String, Attempt> attempts = new LinkedHashMap<>();
        if (input != null && input.toolCalls() != null) {
            for (ToolUseBlock call : input.toolCalls()) {
                if (call == null || !SKILL_TOOL.equals(call.getName())) {
                    continue;
                }
                Map<String, Object> toolInput =
                        call.getInput() == null ? Map.of() : call.getInput();
                String skillRuntimeId = text(toolInput.get("skillId"));
                if (!isMysqlSkill(skillRuntimeId)) {
                    continue;
                }
                String path = text(toolInput.get("path"));
                if (!StringUtils.hasText(path)) {
                    path = "SKILL.md";
                }
                attempts.put(call.getId(), new Attempt(
                        call.getId(),
                        skillRuntimeId,
                        path,
                        LocalDateTime.now()
                ));
            }
        }
        if (attempts.isEmpty()) {
            return next.apply(input);
        }
        return next.apply(input)
                .doOnNext(event -> {
                    if (event instanceof ToolResultTextDeltaEvent delta) {
                        Attempt attempt = attempts.get(delta.getToolCallId());
                        if (attempt != null) {
                            attempt.append(delta.getDelta());
                        }
                    } else if (event instanceof ToolResultEndEvent end) {
                        Attempt attempt = attempts.get(end.getToolCallId());
                        if (attempt != null) {
                            persist(runtimeContext, attempt, end.getState());
                        }
                    }
                })
                .doOnError(error -> attempts.values().forEach(attempt -> {
                    attempt.append(error.getMessage());
                    persist(runtimeContext, attempt, ToolResultState.ERROR);
                }));
    }

    private void persist(
            RuntimeContext runtimeContext,
            Attempt attempt,
            ToolResultState resultState
    ) {
        if (!attempt.persisted.compareAndSet(false, true)) {
            return;
        }
        AgentCallContext callContext = runtimeContext.get(AgentCallContext.class);
        if (callContext == null
                || callContext.getUserInfo() == null
                || callContext.getAgentConfig() == null) {
            log.warn("Skipping skill usage log because the runtime call context is incomplete");
            return;
        }
        UserInfo userInfo = callContext.getUserInfo();
        String skillCode = stripMysqlSuffix(attempt.skillRuntimeId);
        try {
            UserContext.runAs(userInfo, () -> {
                AiSkillInfoEntity skill = skillService.getOne(
                        new LambdaQueryWrapper<AiSkillInfoEntity>()
                                .eq(AiSkillInfoEntity::getTenantId, userInfo.getTenantId())
                                .eq(AiSkillInfoEntity::getSource, skillCode),
                        false
                );
                if (skill == null) {
                    log.warn("Skipping usage log for unknown database skill: {}", skillCode);
                    return;
                }
                boolean success = ToolResultState.SUCCESS.equals(resultState);
                AiSkillLogEntity entity = new AiSkillLogEntity()
                        .setUserId(userInfo.getUserId())
                        .setAgentId(runtimeConfig.getAgentId())
                        .setAgentConfigId(runtimeConfig.getAgentConfigId())
                        .setSessionId(callContext.getSessionId())
                        .setRunId(callContext.getRunId())
                        .setSkillId(skill.getId())
                        .setSkillRuntimeId(attempt.skillRuntimeId)
                        .setToolCallId(attempt.toolCallId)
                        .setOperation("SKILL.md".equalsIgnoreCase(attempt.resourcePath)
                                ? "LOAD_SKILL"
                                : "READ_REFERENCE")
                        .setResourcePath(attempt.resourcePath)
                        .setSuccess(success ? (byte) 1 : (byte) 0)
                        .setErrorMessage(success ? null : attempt.error(resultState))
                        .setStartedAt(attempt.startedAt)
                        .setDurationMs(Math.max(
                                0,
                                Duration.between(attempt.startedAt, LocalDateTime.now()).toMillis()
                        ));
                entity.setTenantId(userInfo.getTenantId());
                logService.save(EntityDefaults.create(entity));
            });
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to persist skill usage log for tool call {}: {}",
                    attempt.toolCallId,
                    e.getMessage()
            );
        }
    }

    private boolean isMysqlSkill(String runtimeId) {
        return StringUtils.hasText(runtimeId) && runtimeId.endsWith(MYSQL_SUFFIX);
    }

    private String stripMysqlSuffix(String runtimeId) {
        return runtimeId.substring(0, runtimeId.length() - MYSQL_SUFFIX.length());
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static final class Attempt {
        private final String toolCallId;
        private final String skillRuntimeId;
        private final String resourcePath;
        private final LocalDateTime startedAt;
        private final StringBuilder output = new StringBuilder();
        private final AtomicBoolean persisted = new AtomicBoolean(false);

        private Attempt(
                String toolCallId,
                String skillRuntimeId,
                String resourcePath,
                LocalDateTime startedAt
        ) {
            this.toolCallId = toolCallId;
            this.skillRuntimeId = skillRuntimeId;
            this.resourcePath = resourcePath;
            this.startedAt = startedAt;
        }

        private void append(String value) {
            if (!StringUtils.hasText(value) || output.length() >= MAX_ERROR_LENGTH) {
                return;
            }
            int remaining = MAX_ERROR_LENGTH - output.length();
            output.append(value, 0, Math.min(remaining, value.length()));
        }

        private String error(ToolResultState state) {
            return output.isEmpty() ? state.getValue() : output.toString();
        }
    }
}
