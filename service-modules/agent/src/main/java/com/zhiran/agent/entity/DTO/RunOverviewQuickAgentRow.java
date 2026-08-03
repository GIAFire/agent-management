package com.zhiran.agent.entity.DTO;

import lombok.Data;

@Data
public class RunOverviewQuickAgentRow {

    private Long id;
    private String agentCode;
    private String agentName;
    private String description;
    private Long modelId;
    private String modelConfigName;
    private String providerName;
    private String modelName;
    private Long runCount30Days;
}
