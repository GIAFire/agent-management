package com.zw.agent.entity.DTO;

import com.zw.agent.factory.modelFactory.ModelType;
import lombok.Data;

@Data
public class AgentConfigDTO {

    private Long agentId;
    private String agentKey;
    private String agentName;
    private String agentDescription;
    private String agentAvatarUrl;
    private String agentType;
    private Integer agentStatus;

    private Long tenantId;
    private String tenantName;
    private Integer tenantStatus;
    private String tenantNacosNamespaceId;
    private String tenantRemark;

    private Long agentConfigId;
    private String sysPrompt;
    private String permissionMode;
    private Integer maxIters;
    private String workspacePath;
    private String contextEnabled;
    private String triggerMode;
    private Integer memoryEnable;
    private Integer planModeEnabled;
    private String planFileDirectory;
    private Integer taskListEnabled;
    private Integer allowShellInPlanMode;
    private Integer planExitApprovalRequired;
    private Integer planMaxSteps;
    private Integer planAutoEnterEnabled;
    private String planPrompt;

    private Integer triggerMessages;
    private Integer keepMessages;
    private Integer triggerTokens;
    private Integer keepTokens;
    private  Boolean truncateArgsEnabled;
    private  Boolean toolResultEvictionEnabled;

    private Long modelId;
    private ModelType provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean isStream;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Long timeoutMs;
    private Integer maxAttempts;
    private Long fallbackModelConfigId;
    private Integer modelStatus;

}
