package com.zw.agent.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@AllArgsConstructor
@Data
public class AgentStreamResponse {
    private Long runId;
    private String eventType;
    private String delta;
    private Long seq;
    private Integer usageToken;
    private Double usageTime;

    public AgentStreamResponse(Long runId, String eventType, String delta, Long seq) {
    	this.runId = runId;
    	this.eventType = eventType;
    	this.delta = delta;
    	this.seq = seq;
    }

    public AgentStreamResponse(Long runId, String eventType, Integer usageToken, Double usageTime) {
        this.runId = runId;
        this.eventType = eventType;
        this.usageToken = usageToken;
        this.usageTime = usageTime;
    }
}
