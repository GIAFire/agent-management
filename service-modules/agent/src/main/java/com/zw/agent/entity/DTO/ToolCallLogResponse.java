package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ToolCallLogResponse(
        Long id,
        Long runId,
        Long sessionId,
        Long agentId,
        Long toolId,
        String toolName,
        String toolCallId,
        String permissionBehavior,
        String successStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endedAt,
        Long durationMs
) {
}
