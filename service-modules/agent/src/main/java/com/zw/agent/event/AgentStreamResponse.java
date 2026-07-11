package com.zw.agent.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@AllArgsConstructor
@Data
public class AgentStreamResponse {
    private Long runId;
    private String eventType;
    /**
     * 事件来源路径：
     * null = 父Agent
     * main/researcher = 子Agent
     */
    private String sourcePath;
    /**
     * 子Agent名称，从 sourcePath 里解析，例如 researcher
     */
    private String subAgentName;
    Long subAgentInstanceId;
    Long subAgentTaskId;
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
