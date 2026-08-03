package com.zhiran.agent.entity.DTO;

import com.zhiran.agent.constant.enumeration.ModelProtocol;
import com.zhiran.agent.constant.enumeration.StateStoreType;
import lombok.Data;

import java.util.List;

@Data
public class AgentConfigDTO {

    private Long agentId;
    private String agentCode;
    private String agentName;
    private String agentDescription;
    private String agentAvatarUrl;
    private String agentType;

    private Long tenantId;
    private String tenantName;
    private Integer tenantStatus;
    private String tenantNacosNamespaceId;
    private String tenantRemark;

    private Long agentConfigId;
    private Long sysPromptId;
    private String permissionMode;
    private Integer maxIters;
    private Integer compactionEnabled;
    private Integer memoryEnable;
    private Integer planModeEnabled;
    private String planFileDirectory;
    private Integer taskListEnabled;
    private Integer allowShellInPlanMode;
    private StateStoreType stateStoreType;

    private Integer triggerMessages;
    private Integer keepMessages;
    private Integer triggerTokens;
    private Integer keepTokens;
    private Boolean toolResultEvictionEnabled;

    private Long modelId;
    private String modelConfigName;
    private String providerName;
    private ModelProtocol protocol;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean streaming;
    private Boolean thinking;
    private Integer thinkingBudget;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Long timeoutMs;
    private Integer maxAttempts;
    private Long fallbackModelConfigId;
    private Integer modelStatus;
    private Integer modelDeleted;

    private String promptName;
    private String sysPrompt;

    private List<Long> selectedToolIds;
    private List<Long> selectedSkillIds;
    private List<Long> selectedKnowledgeBaseIds;
    private List<Long> selectedSubagentIds;

}
