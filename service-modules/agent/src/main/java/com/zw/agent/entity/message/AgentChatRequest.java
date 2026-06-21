package com.zw.agent.entity.message;

import lombok.Data;

@Data
public class AgentChatRequest {
    private Long agentId;
    private String sessionId;
    private String content;
}
