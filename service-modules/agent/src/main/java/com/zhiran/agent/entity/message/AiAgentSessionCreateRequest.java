package com.zhiran.agent.entity.message;

import lombok.Data;

@Data
public class AiAgentSessionCreateRequest {
    private Long agentId;
    private String title;
}
