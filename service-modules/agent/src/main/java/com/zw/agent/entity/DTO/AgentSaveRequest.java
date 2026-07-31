package com.zw.agent.entity.DTO;

import com.zw.agent.constant.enumeration.StateStoreType;
import java.util.List;
import lombok.Data;

@Data
public class AgentSaveRequest {

    private String agentCode;
    private String agentName;
    private String description;

    private Integer agentVersion;
    private Integer configVersion;

    private Long modelId;
    private Long sysPromptId;
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

    private List<Long> selectedToolIds;
    private List<Long> selectedSkillIds;
    private List<Long> selectedKnowledgeBaseIds;
    private List<Long> selectedSubagentIds;
}
