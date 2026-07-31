package com.zw.agent.entity.DTO;

import java.time.LocalDateTime;

public record AgentRecentRunResponse(
        Long id,
        Long agentId,
        String agentName,
        String status,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationMs
) {
}
