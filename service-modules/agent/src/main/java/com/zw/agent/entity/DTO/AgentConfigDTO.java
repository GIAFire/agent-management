package com.zw.agent.entity.DTO;

import com.zw.agent.factory.RAGFactory.EmbeddinModelType;
import com.zw.agent.factory.modelFactory.ModelType;
import io.agentscope.core.permission.PermissionMode;
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
    private String permissionMode;
    private Integer maxIters;
    private String workspacePath;
    private String contextEnabled;
    private String triggerMode;
    // ========== Compaction上下文压缩 字段 ==========
    private Integer triggerMessages;
    private Integer keepMessages;
    private Integer triggerTokens;
    private Integer keepTokens;
    private  Boolean truncateArgsEnabled;   // 是否启用工具参数预截断：1启用，0关闭
    private  Boolean toolResultEvictionEnabled; // 是否启用大工具结果卸载：1启用，0关闭
    // 兼容性字段

    // ========== Model 字段 ==========
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

    // ========== knowledge 字段 ==========
    private EmbeddinModelType storeType;

}
