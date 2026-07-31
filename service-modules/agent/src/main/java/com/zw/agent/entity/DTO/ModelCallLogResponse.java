package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ModelCallLogResponse(
        Long id,
        Long modelConfigId,
        Long runId,
        Long sessionId,
        Long agentId,
        Long agentConfigId,
        String callSource,
        String sourcePath,
        String status,
        String configName,
        String providerName,
        String protocol,
        String modelName,
        Integer inputTokens,
        Integer outputTokens,
        Integer cachedTokens,
        Integer totalTokens,
        Long durationMs,
        String errorCode,
        String errorMessage,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime startedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime endedAt
) {
}
