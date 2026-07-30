package com.zw.agent.entity.DTO;

public record ToolMetricsResponse(
        long availableTools,
        long enabledTools,
        long enabledGroups,
        long todayCalls,
        Double callChangePercent,
        Double successRate,
        long failedCalls
) {
}
