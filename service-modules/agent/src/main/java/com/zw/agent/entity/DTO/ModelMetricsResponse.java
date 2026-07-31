package com.zw.agent.entity.DTO;

public record ModelMetricsResponse(
        long total,
        long enabled,
        long todayCalls,
        Double callChangePercent,
        Double successRate,
        long failedCalls,
        Double averageDurationMs,
        Double averageDurationChangeMs
) {
}
