package com.zhiran.agent.entity.DTO;

public record AgentBoundResourceResponse(
        Long id,
        String name,
        boolean available
) {
}
