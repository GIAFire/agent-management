package com.zhiran.agent.entity.DTO;

import java.time.LocalDateTime;

public record AgentListItemResponse(
        Long id,
        String agentCode,
        String agentName,
        String description,
        Long modelId,
        String modelConfigName,
        String protocol,
        String modelName,
        long subagentCount,
        long todayRuns,
        Double todaySuccessRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
