package com.zw.agent.entity.DTO;

import java.time.LocalDateTime;

public record SysPromptDetailResponse(
        Long id,
        String promptName,
        String description,
        String sysPrompt,
        long contentLength,
        long bindingCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
