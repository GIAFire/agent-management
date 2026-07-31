package com.zw.agent.entity.DTO;

import java.util.List;

public record AgentRecentRunsResponse(
        List<AgentRecentRunResponse> completed,
        List<AgentRecentRunResponse> failed
) {
}
