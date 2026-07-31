package com.zw.agent.service;

import com.zw.agent.entity.AiModelCallLogEntity;
import com.zw.agent.mapper.AiModelCallLogMapper;
import com.zw.agent.runtime.model.ModelAuditDescriptor;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.model.ChatUsage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModelCallAuditService {

    public static final String SOURCE_AGENT_RUN = "AGENT_RUN";
    public static final String SOURCE_MANUAL_TEST = "MANUAL_TEST";

    private final AiModelCallLogMapper callLogMapper;

    public AiModelCallLogEntity start(
            ModelAuditDescriptor descriptor,
            Long runId,
            Long sessionId,
            String callSource
    ) {
        AiModelCallLogEntity entity = new AiModelCallLogEntity()
                .setModelConfigId(descriptor.modelConfigId())
                .setRunId(runId)
                .setSessionId(sessionId)
                .setAgentId(descriptor.agentId())
                .setAgentConfigId(descriptor.agentConfigId())
                .setCallSource(callSource)
                .setSourcePath(descriptor.sourcePath())
                .setStatus("RUNNING")
                .setConfigNameSnapshot(descriptor.configName())
                .setProviderNameSnapshot(descriptor.providerName())
                .setProtocolSnapshot(descriptor.protocol())
                .setModelNameSnapshot(descriptor.modelName())
                .setStartedAt(LocalDateTime.now());
        entity.setTenantId(descriptor.userInfo().getTenantId());
        return asUser(descriptor.userInfo(), () -> {
            callLogMapper.insert(EntityDefaults.create(entity));
            return entity;
        });
    }

    public void success(
            ModelAuditDescriptor descriptor,
            AiModelCallLogEntity entity,
            ChatUsage usage
    ) {
        finish(descriptor, entity, "SUCCESS", usage, null);
    }

    public void failed(
            ModelAuditDescriptor descriptor,
            AiModelCallLogEntity entity,
            Throwable error
    ) {
        finish(descriptor, entity, "FAILED", null, error);
    }

    public void cancelled(
            ModelAuditDescriptor descriptor,
            AiModelCallLogEntity entity
    ) {
        finish(descriptor, entity, "CANCELLED", null, null);
    }

    private void finish(
            ModelAuditDescriptor descriptor,
            AiModelCallLogEntity entity,
            String status,
            ChatUsage usage,
            Throwable error
    ) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        LocalDateTime endedAt = LocalDateTime.now();
        entity.setStatus(status)
                .setEndedAt(endedAt)
                .setDurationMs(Math.max(
                        0L,
                        Duration.between(entity.getStartedAt(), endedAt).toMillis()
                ));
        if (usage != null) {
            entity.setInputTokens(usage.getInputTokens())
                    .setOutputTokens(usage.getOutputTokens())
                    .setCachedTokens(usage.getCachedTokens())
                    .setTotalTokens(usage.getTotalTokens());
        }
        if (error != null) {
            entity.setErrorCode(limit(error.getClass().getSimpleName(), 128))
                    .setErrorMessage(limit(
                            redact(safeMessage(error), descriptor.secretValues()),
                            1000
                    ));
        }
        asUser(descriptor.userInfo(), () -> {
            callLogMapper.updateById(EntityDefaults.update(entity));
            return null;
        });
    }

    private String safeMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private String redact(String value, List<String> secrets) {
        String result = value == null ? "" : value;
        if (secrets == null) {
            return result;
        }
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                result = result.replace(secret, "***");
            }
        }
        return result;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> T asUser(UserInfo userInfo, java.util.function.Supplier<T> supplier) {
        return UserContext.callAs(userInfo, supplier);
    }
}
