package com.zhiran.agent.entity.DTO;

public record ModelCandidateResponse(
        Long id,
        String configName,
        String protocol,
        String modelName,
        Integer maxTokens,
        Integer status
) {
}
