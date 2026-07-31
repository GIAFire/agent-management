package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ModelDetailResponse(
        Long id,
        String configName,
        String providerName,
        String protocol,
        String baseURL,
        String apiKey,
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
        List<ModelHeaderResponse> headers,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt
) {
}
