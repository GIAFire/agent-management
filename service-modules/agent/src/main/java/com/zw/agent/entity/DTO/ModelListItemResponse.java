package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelListItemResponse(
        Long id,
        String configName,
        String providerName,
        String protocol,
        String description,
        String modelName,
        Integer streaming,
        Integer thinking,
        BigDecimal temperature,
        BigDecimal topP,
        Integer maxTokens,
        Long timeoutMs,
        Integer thinkingBudget,
        Integer maxAttempts,
        Integer status,
        long todayCalls,
        String lastTestStatus,
        Long lastTestDurationMs,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastTestAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt
) {
}
