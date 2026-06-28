package com.zw.agent.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AgentStreamResponse {
    private Long runId;
    private String eventType;
    private String delta;
    private Long seq;
    private Integer usageToken;
    private Double usageTime;
    private Object payload;

    public AgentStreamResponse(Long runId, String eventType, String delta, Long seq) {
        this(runId, eventType, delta, seq, null);
    }

    public AgentStreamResponse(Long runId, String eventType, String delta, Long seq, Object payload) {
    	this.runId = runId;
    	this.eventType = eventType;
    	this.delta = delta;
    	this.seq = seq;
        this.payload = payload;
    }

    public AgentStreamResponse(Long runId, String eventType, Integer usageToken, Double usageTime) {
        this.runId = runId;
        this.eventType = eventType;
        this.usageToken = usageToken;
        this.usageTime = usageTime;
    }
}
