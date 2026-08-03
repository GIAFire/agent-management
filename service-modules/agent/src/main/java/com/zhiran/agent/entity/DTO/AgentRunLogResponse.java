package com.zhiran.agent.entity.DTO;

import java.time.LocalDateTime;

public record AgentRunLogResponse(
        Long id,
        Long sessionId,
        Long agentId,
        Long agentConfigId,
        String status,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationMs
) {
}
