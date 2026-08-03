package com.zhiran.agent.entity.DTO;

public record SysPromptMetricsResponse(
        long total,
        long newToday,
        long boundPrompts,
        long boundAgents
) {
}
