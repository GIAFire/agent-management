package com.zhiran.agent.entity.DTO;

public record AgentMetricsResponse(
        long totalAgents,
        long newToday,
        long todayTokens,
        Double tokenChangePercent,
        long todayRuns,
        Double runChangePercent,
        Double successRate,
        Double successRateChange,
        Double averageDurationMs
) {
}
