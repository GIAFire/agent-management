package com.zw.agent.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AgentStreamResponse {
    private Long runId;
    private String eventType;
    private String delta;
//    private Object payload;
}
