package com.zhiran.agent.entity.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record SysPromptAnalyticsResponse(
        List<BindingRanking> bindingRanking,
        List<RecentPrompt> recentlyUpdated
) {
    public record BindingRanking(
            Long id,
            String promptName,
            long bindingCount
    ) {
    }

    public record RecentPrompt(
            Long id,
            String promptName,
            long contentLength,
            LocalDateTime updatedAt
    ) {
    }
}
