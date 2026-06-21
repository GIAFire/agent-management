package com.zw.agent.entity.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentConfigDTO {
    // ========== Agent 字段 ==========
    private Long agentId;
    private String agentKey;
    private String agentName;
    private String agentDescription;
    private String agentAvatarUrl;
    private String agentType;
    private Integer agentStatus;

    // ========== Tenant 字段 ==========
    private Long tenantId;
    private String tenantName;
    private Integer tenantStatus;
    private String tenantNacosNamespaceId;
    private String tenantRemark;

    // ========== AgentConfig 字段 ==========
    private Long agentConfigId;
    private String sysPrompt;
    private Long modelConfigId;
    private Integer maxIters;
    private String compactionConfigJson;
    private String workspaceConfigJson;
    // 兼容性字段
    private String workspacePath;
    private Integer compactionTriggerMessages;
    private Integer compactionKeepMessages;

    private String permissionMode;
    private String visualSchemaJson;
    private Integer publishStatus;
    private LocalDateTime publishedAt;

    // ========== ModelConfig 字段 ==========
    private Long modelId;
    private String provider;
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
