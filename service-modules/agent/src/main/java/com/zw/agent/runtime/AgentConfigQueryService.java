package com.zw.agent.runtime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zw.agent.entity.*;
import com.zw.agent.exception.AgentConfigException;
import com.zw.agent.mapper.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Agent配置查询服务类，负责从数据库中加载已发布的Agent运行时配置。
 * 通过租户编码、Agent ID等参数，级联查询租户、Agent定义、版本信息、模型配置等数据，
 * 构建完整的AgentRuntimeConfig对象供运行时使用。
 */
@AllArgsConstructor
@Service
public class AgentConfigQueryService {

    private static final int ENABLED = 1;
    private static final int PUBLISHED = 1;

    private final SysTenantMapper tenantMapper;
    private final AiAgentMapper agentDefinitionMapper;
    private final AiAgentConfigMapper agentVersionMapper;
    private final AiModelConfigMapper modelConfigMapper;
    private ObjectMapper objectMapper;


    /**
     * 加载已发布的Agent运行时配置。
     * 该方法会依次验证并加载：启用的租户、启用的Agent定义、已发布的Agent版本、启用的模型配置，
     * 并从JSON配置中提取工作空间路径和消息压缩配置，最终构建完整的AgentRuntimeConfig对象。
     *
     * @param tenantCode 租户编码，用于标识多租户环境中的具体租户
     * @param agentId Agent唯一标识ID，用于定位具体的Agent实例
     * @return 完整的Agent运行时配置对象，包含租户ID、Agent信息、模型配置、工作空间路径、压缩策略等
     * @throws AgentConfigException 当租户不存在或已停用、Agent不存在或未发布、模型配置无效时抛出异常
     */
    public AgentRuntimeConfig loadPublishedConfig(String tenantCode, Long agentId) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new AgentConfigException("tenantCode 不能为空");
        }
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }

        // 查询租户信息、Agent定义、Agent配置信息和模型配置
        SysTenantEntity tenant = loadEnabledTenant(tenantCode);
        AiAgentEntity agent = loadEnabledAgentDefinition(tenant.getId(), agentId);
        AiAgentConfigEntity AgentConfig = loadPublishedAgentVersion(
                tenant.getId(),
                agent.getId(),
                agent.getAgentConfigId()
        );
        AiModelConfigEntity modelConfig = loadEnabledModelConfig(
                tenant.getId(),
                AgentConfig.getModelConfigId()
        );

        // 从JSON配置中提取工作空间路径，若未配置则使用默认路径
        String workspacePath = readText(
                AgentConfig.getWorkspaceConfigJson(),
                defaultWorkspacePath(tenantCode, agentId, AgentConfig.getId()),
                "workspacePath",
                "path"
        );

        // 从JSON配置中提取消息压缩触发阈值和保留数量
        int compactionTriggerMessages = readInt(
                AgentConfig.getCompactionConfigJson(),
                20,
                "triggerMessages",
                "compactionTriggerMessages"
        );

        int compactionKeepMessages = readInt(
                AgentConfig.getCompactionConfigJson(),
                8,
                "keepMessages",
                "compactionKeepMessages"
        );

        return new AgentRuntimeConfig(
                tenant.getId(),
                agent.getId(),
                AgentConfig.getId(),
                agent.getAgentName(),
                AgentConfig.getSysPrompt(),
                modelConfig.getProvider(),
                modelConfig.getModelName(),
                workspacePath,
                compactionTriggerMessages,
                compactionKeepMessages
        );
    }

    /**
     * 加载已启用状态的租户实体。
     * 根据租户编码查询租户信息，并验证租户处于启用状态。
     *
     * @param tenantCode 租户编码，用于唯一标识租户
     * @return 租户实体对象，包含租户ID、编码、状态等信息
     * @throws AgentConfigException 当租户不存在或状态为非启用时抛出异常
     */
    private SysTenantEntity loadEnabledTenant(String tenantCode) {
        SysTenantEntity tenant = tenantMapper.selectOne(
                Wrappers.<SysTenantEntity>lambdaQuery()
                        .select(
                                SysTenantEntity::getId,
                                SysTenantEntity::getTenantCode,
                                SysTenantEntity::getStatus
                        )
                        .eq(SysTenantEntity::getTenantCode, tenantCode)
                        .eq(SysTenantEntity::getStatus, ENABLED)
                        .last("LIMIT 1")
        );

        if (tenant == null) {
            throw new AgentConfigException("租户不存在或已停用: " + tenantCode);
        }

        return tenant;
    }

    /**
     * 加载已启用状态的Agent定义实体。
     * 根据租户ID和Agent ID查询Agent定义，并验证Agent处于启用状态且已发布版本。
     *
     * @param tenantId 租户ID，用于限定查询范围
     * @param agentId Agent ID，用于定位具体的Agent定义
     * @return Agent定义实体对象，包含Agent名称、当前版本ID、状态等信息
     * @throws AgentConfigException 当Agent不存在、状态为非启用或未发布版本时抛出异常
     */
    private AiAgentEntity loadEnabledAgentDefinition(Long tenantId, Long agentId) {
        AiAgentEntity agent = agentDefinitionMapper.selectOne(
                Wrappers.<AiAgentEntity>lambdaQuery()
                        .select(
                                AiAgentEntity::getId,
                                AiAgentEntity::getTenantId,
                                AiAgentEntity::getAgentName,
                                AiAgentEntity::getAgentConfigId,
                                AiAgentEntity::getStatus
                        )
                        .eq(AiAgentEntity::getTenantId, tenantId)
                        .eq(AiAgentEntity::getId, agentId)
                        .eq(AiAgentEntity::getStatus, ENABLED)
                        .last("LIMIT 1")
        );

        if (agent == null) {
            throw new AgentConfigException("Agent 不存在或已停用: " + agentId);
        }

        if (agent.getAgentConfigId() == null) {
            throw new AgentConfigException("Agent 尚未发布版本: " + agentId);
        }

        return agent;
    }

    /**
     * 加载已发布状态的Agent版本实体。
     * 根据租户ID、Agent ID和版本ID查询Agent版本信息，并验证版本处于已发布状态。
     *
     * @param tenantId 租户ID，用于限定查询范围
     * @param agentId Agent ID，用于定位具体的Agent
     * @param agentConfigId 当前版本ID，用于查询具体的版本记录
     * @return Agent版本实体对象，包含系统提示词、模型配置ID、压缩配置JSON、工作空间配置JSON等
     * @throws AgentConfigException 当版本不存在或状态为非发布时抛出异常
     */
    private AiAgentConfigEntity loadPublishedAgentVersion(
            Long tenantId,
            Long agentId,
            Long agentConfigId
    ) {
        AiAgentConfigEntity agentConfig = agentVersionMapper.selectOne(
                Wrappers.<AiAgentConfigEntity>lambdaQuery()
                        .select(
                                AiAgentConfigEntity::getId,
                                AiAgentConfigEntity::getTenantId,
                                AiAgentConfigEntity::getAgentId,
                                AiAgentConfigEntity::getSysPrompt,
                                AiAgentConfigEntity::getModelConfigId,
                                AiAgentConfigEntity::getCompactionConfigJson,
                                AiAgentConfigEntity::getWorkspaceConfigJson,
                                AiAgentConfigEntity::getPublishStatus
                        )
                        .eq(AiAgentConfigEntity::getTenantId, tenantId)
                        .eq(AiAgentConfigEntity::getAgentId, agentId)
                        .eq(AiAgentConfigEntity::getId, agentConfigId)
                        .eq(AiAgentConfigEntity::getPublishStatus, PUBLISHED)
                        .last("LIMIT 1")
        );

        if (agentConfig == null) {
            throw new AgentConfigException("Agent 当前配置不存在或未启用: " + agentConfigId);
        }

        return agentConfig;
    }

    /**
     * 加载已启用状态的模型配置实体。
     * 根据租户ID和模型配置ID查询模型配置，并验证模型处于启用状态且配置完整。
     *
     * @param tenantId 租户ID，用于限定查询范围
     * @param modelConfigId 模型配置ID，用于定位具体的模型配置记录
     * @return 模型配置实体对象，包含提供商、模型名称、基础URL、API密钥等配置信息
     * @throws AgentConfigException 当模型配置ID为空、配置不存在、状态为非启用或配置不完整时抛出异常
     */
    private AiModelConfigEntity loadEnabledModelConfig(Long tenantId, Long modelConfigId) {
        if (modelConfigId == null) {
            throw new AgentConfigException("Agent 版本未配置模型");
        }

        AiModelConfigEntity modelConfig = modelConfigMapper.selectOne(
                Wrappers.<AiModelConfigEntity>lambdaQuery()
                        .select(
                                AiModelConfigEntity::getId,
                                AiModelConfigEntity::getTenantId,
                                AiModelConfigEntity::getBaseURL,
                                AiModelConfigEntity::getApiKeyCipher,
                                AiModelConfigEntity::getProvider,
                                AiModelConfigEntity::getModelName,
                                AiModelConfigEntity::getStatus
                        )
                        .eq(AiModelConfigEntity::getTenantId, tenantId)
                        .eq(AiModelConfigEntity::getId, modelConfigId)
                        .eq(AiModelConfigEntity::getStatus, ENABLED)
                        .last("LIMIT 1")
        );

        if (modelConfig == null) {
            throw new AgentConfigException("模型配置不存在或已停用: " + modelConfigId);
        }

        if (!StringUtils.hasText(modelConfig.getProvider())) {
            throw new AgentConfigException("模型配置 provider 不能为空: " + modelConfigId);
        }

        if (!StringUtils.hasText(modelConfig.getModelName())) {
            throw new AgentConfigException("模型配置 modelName 不能为空: " + modelConfigId);
        }

        return modelConfig;
    }

    /**
     * 生成默认的Agent工作空间路径。
     * 根据租户编码、Agent ID和版本ID构建文件系统路径，用于存储Agent的临时文件和运行时数据。
     *
     * @param tenantCode 租户编码，用于隔离不同租户的工作空间
     * @param agentId Agent ID，用于区分不同的Agent
     * @param versionId 版本ID，用于区分不同的Agent版本
     * @return 默认的工作空间路径字符串，格式为 /tmp/agentscope/workspaces/{tenantCode}/agent-{agentId}/version-{versionId}
     */
    private String defaultWorkspacePath(String tenantCode, Long agentId, Long versionId) {
        return "/tmp/agentscope/workspaces/"
                + tenantCode
                + "/agent-"
                + agentId
                + "/version-"
                + versionId;
    }

    /**
     * 从JSON字符串中读取整数值。
     * 支持按字段名层级查找，返回第一个匹配的数字字段的值。若JSON为空或解析失败，则返回默认值。
     *
     * @param json JSON字符串，包含目标整数字段
     * @param defaultValue 默认值，当JSON为空、字段不存在或解析失败时返回
     * @param fieldNames 字段名称列表，按顺序尝试从JSON中提取整数值
     * @return 解析得到的整数值，或解析失败时的默认值
     * @throws AgentConfigException 当JSON格式错误导致解析异常时抛出异常
     */
    private int readInt(String json, int defaultValue, String... fieldNames) {
        if (!StringUtils.hasText(json)) {
            return defaultValue;
        }

        try {
            tools.jackson.databind.JsonNode root = objectMapper.readTree(json);
            for (String fieldName : fieldNames) {
                tools.jackson.databind.JsonNode node = root.get(fieldName);
                if (node != null && node.isNumber()) {
                    return node.asInt();
                }
            }
            return defaultValue;
        } catch (Exception e) {
            throw new AgentConfigException("解析 compaction_config_json 失败: " + json, e);
        }
    }

    /**
     * 从JSON字符串中读取文本值。
     * 支持按字段名层级查找，返回第一个匹配的非空文本字段的值。若JSON为空或解析失败，则返回默认值。
     *
     * @param json JSON字符串，包含目标文本字段
     * @param defaultValue 默认值，当JSON为空、字段不存在或解析失败时返回
     * @param fieldNames 字段名称列表，按顺序尝试从JSON中提取文本值
     * @return 解析得到的文本字符串，或解析失败时的默认值
     * @throws AgentConfigException 当JSON格式错误导致解析异常时抛出异常
     */
    private String readText(String json, String defaultValue, String... fieldNames) {
        if (!StringUtils.hasText(json)) {
            return defaultValue;
        }

        try {
            tools.jackson.databind.JsonNode root = objectMapper.readTree(json);
            for (String fieldName : fieldNames) {
                JsonNode node = root.get(fieldName);
                if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                    return node.asText();
                }
            }
            return defaultValue;
        } catch (Exception e) {
            throw new AgentConfigException("解析 workspace_config_json 失败: " + json, e);
        }
    }
}