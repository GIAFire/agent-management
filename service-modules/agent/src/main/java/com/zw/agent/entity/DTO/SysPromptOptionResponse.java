package com.zw.agent.entity.DTO;

public record SysPromptOptionResponse(
        Long id,
        String promptName,
        String description,
        String sysPrompt
) {
}
