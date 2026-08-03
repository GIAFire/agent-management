package com.zhiran.agent.entity.DTO;

import com.zhiran.agent.constant.enumeration.StateStoreType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentDetailResponse {

    private Long id;
    private String agentCode;
    private String agentName;
    private String description;
    private Integer agentVersion;
    private Long agentConfigId;
    private Integer configVersion;

    private Long modelId;
    private String modelConfigName;
    private String providerName;
    private String protocol;
    private String modelName;

    private Long sysPromptId;
    private String sysPromptName;
    private String sysPrompt;
    private boolean systemPromptAvailable;

    private Integer maxIters;
    private String permissionMode;
    private Integer compactionEnabled;
    private Integer triggerMessages;
    private Integer keepMessages;
    private Integer triggerTokens;
    private Integer keepTokens;
    private Integer toolResultEvictionEnabled;
    private Integer memoryEnable;
    private Integer planModeEnabled;
    private String planFileDirectory;
    private Integer taskListEnabled;
    private Integer allowShellInPlanMode;
    private StateStoreType stateStoreType;

    private List<AgentBoundResourceResponse> tools;
    private List<AgentBoundResourceResponse> skills;
    private List<AgentBoundResourceResponse> knowledgeBases;
    private List<AgentBoundResourceResponse> subagents;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
