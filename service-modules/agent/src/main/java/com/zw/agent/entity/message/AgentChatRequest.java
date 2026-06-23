package com.zw.agent.entity.message;

import lombok.Data;

@Data
public class AgentChatRequest {
    private Long agentId;
    private Long sessionId;
    private String content;
}
