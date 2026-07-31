package com.zw.agent.entity.DTO;

public record ModelCandidateResponse(
        Long id,
        String configName,
        String providerName,
        String protocol,
        String modelName,
        Integer maxTokens,
        Integer status
) {
}
