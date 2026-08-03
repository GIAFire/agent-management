package com.zhiran.agent.entity.DTO;

import lombok.Data;

@Data
public class AgentRunSummary {

    private Long agentId;
    private Long totalRuns;
    private Long successRuns;
    private Long failedRuns;
    private Long cancelledRuns;
    private Double averageDurationMs;
}
