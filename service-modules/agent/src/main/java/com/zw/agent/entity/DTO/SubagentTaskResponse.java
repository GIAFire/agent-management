package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SubagentTaskResponse(
        Long id,
        Long subagentId,
        String subagentName,
        Long parentAgentId,
        String parentAgentName,
        String taskInput,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
        Long durationMs,
        String errorMessage
) {
}
