package com.zw.agent.entity.DTO;

public record ModelTestResponse(
        boolean success,
        Long durationMs,
        String modelName,
        String replyPreview,
        String errorCode,
        String errorMessage
) {
}
