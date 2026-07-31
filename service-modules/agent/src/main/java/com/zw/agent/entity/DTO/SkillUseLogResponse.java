package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SkillUseLogResponse(
        Long id,
        Long skillId,
        String skillCode,
        String skillName,
        Long agentId,
        String agentName,
        String operation,
        String resourcePath,
        Byte success,
        String errorMessage,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
        Long durationMs
) {
}
