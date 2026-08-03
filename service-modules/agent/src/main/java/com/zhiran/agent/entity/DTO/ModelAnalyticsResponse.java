package com.zhiran.agent.entity.DTO;

import java.time.LocalDate;
import java.util.List;

public record ModelAnalyticsResponse(
        int days,
        List<TrendPoint> trend,
        List<ProviderDistribution> providerDistribution
) {
    public record TrendPoint(LocalDate date, long calls) {
    }

    public record ProviderDistribution(
            String providerName,
            long modelCount,
            double percent
    ) {
    }
}
