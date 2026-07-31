package com.zw.agent.entity.DTO;

public record AgentBoundResourceResponse(
        Long id,
        String name,
        boolean available
) {
}
