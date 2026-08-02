package com.zw.agent.entity.DTO;

import java.time.LocalDate;
import java.util.List;

public record RunOverviewResponse(
        Metrics metrics,
        Trend trend,
        List<AgentRecentRunResponse> recentRuns,
        List<QuickAgent> quickAgents
) {

    public record Metrics(
            long totalAgents,
            long newToday,
            long todayRuns,
            Double runChangePercent,
            Double successRate,
            Double successRateChange,
            Double averageDurationMs,
            Double averageDurationChangeMs
    ) {
    }

    public record Trend(
            LocalDate startDate,
            LocalDate endDate,
            List<TrendPoint> points,
            TrendSummary summary
    ) {
    }

    public record TrendPoint(
            LocalDate date,
            long runCount,
            long successCount
    ) {
    }

    public record TrendSummary(
            long totalRuns,
            Double totalRunsChangePercent,
            long successRuns,
            Double successRunsChangePercent,
            Double successRate,
            Double successRateChange,
            Double averageDurationMs,
            Double averageDurationChangeMs
    ) {
    }

    public record QuickAgent(
            Long id,
            String agentCode,
            String agentName,
            String description,
            Long modelId,
            String modelConfigName,
            String providerName,
            String modelName,
            long runCount30Days
    ) {
    }
}
