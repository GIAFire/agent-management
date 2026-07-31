package com.zw.agent.entity.DTO;

import lombok.Data;

@Data
public class ModelCallSummary {
    private Long totalCalls;
    private Long successCalls;
    private Long failedCalls;
    private Double averageDurationMs;
}
