package com.zhiran.agent.entity.DTO;

import java.time.LocalDateTime;

public record SysPromptListItemResponse(
        Long id,
        String promptName,
        String description,
        String contentPreview,
        long contentLength,
        long bindingCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
