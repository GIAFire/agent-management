package com.zw.agent.entity.DTO;

import com.zw.agent.factory.modelFactory.ModelType;
import io.agentscope.core.permission.PermissionMode;
import lombok.Data;

import java.time.LocalDateTime;

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
