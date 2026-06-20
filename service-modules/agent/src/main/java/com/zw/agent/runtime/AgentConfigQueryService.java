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

@AllArgsConstructor
@Service
public class AgentConfigQueryService {

    private static final int ENABLED = 1;
    private static final int PUBLISHED = 1;

    private final SysTenantMapper tenantMapper;
    private final AiAgentDefinitionMapper agentDefinitionMapper;
    private final AiAgentVersionMapper agentVersionMapper;
    private final AiModelConfigMapper modelConfigMapper;
    private final AiLlmCredentialMapper llmCredentialMapper;
    private ObjectMapper objectMapper;


    public AgentRuntimeConfig loadPublishedConfig(String tenantCode, Long agentId) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new AgentConfigException("tenantCode 不能为空");
        }
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }

        SysTenantEntity tenant = loadEnabledTenant(tenantCode);
        AiAgentDefinitionEntity agentDefinition = loadEnabledAgentDefinition(tenant.getId(), agentId);
        AiAgentVersionEntity agentVersion = loadPublishedAgentVersion(
                tenant.getId(),
                agentDefinition.getId(),
                agentDefinition.getCurrentVersionId()
        );
        AiModelConfigEntity modelConfig = loadEnabledModelConfig(
                tenant.getId(),
                agentVersion.getModelConfigId()
        );

        validateCredentialIfNecessary(tenant.getId(), modelConfig);

        String workspacePath = readText(
                agentVersion.getWorkspaceConfigJson(),
                defaultWorkspacePath(tenantCode, agentId, agentVersion.getId()),
                "workspacePath",
                "path"
        );

        int compactionTriggerMessages = readInt(
                agentVersion.getCompactionConfigJson(),
                20,
                "triggerMessages",
                "compactionTriggerMessages"
        );

        int compactionKeepMessages = readInt(
                agentVersion.getCompactionConfigJson(),
                8,
                "keepMessages",
                "compactionKeepMessages"
        );

        return new AgentRuntimeConfig(
                tenant.getId(),
                agentDefinition.getId(),
                agentVersion.getId(),
                agentDefinition.getAgentName(),
                agentVersion.getSysPrompt(),
                modelConfig.getProvider(),
                modelConfig.getModelName(),
                workspacePath,
                compactionTriggerMessages,
                compactionKeepMessages
        );
    }

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

    private AiAgentDefinitionEntity loadEnabledAgentDefinition(Long tenantId, Long agentId) {
        AiAgentDefinitionEntity agentDefinition = agentDefinitionMapper.selectOne(
                Wrappers.<AiAgentDefinitionEntity>lambdaQuery()
                        .select(
                                AiAgentDefinitionEntity::getId,
                                AiAgentDefinitionEntity::getTenantId,
                                AiAgentDefinitionEntity::getAgentName,
                                AiAgentDefinitionEntity::getCurrentVersionId,
                                AiAgentDefinitionEntity::getStatus
                        )
                        .eq(AiAgentDefinitionEntity::getTenantId, tenantId)
                        .eq(AiAgentDefinitionEntity::getId, agentId)
                        .eq(AiAgentDefinitionEntity::getStatus, ENABLED)
                        .last("LIMIT 1")
        );

        if (agentDefinition == null) {
            throw new AgentConfigException("Agent 不存在或已停用: " + agentId);
        }

        if (agentDefinition.getCurrentVersionId() == null) {
            throw new AgentConfigException("Agent 尚未发布版本: " + agentId);
        }

        return agentDefinition;
    }

    private AiAgentVersionEntity loadPublishedAgentVersion(
            Long tenantId,
            Long agentId,
            Long currentVersionId
    ) {
        AiAgentVersionEntity agentVersion = agentVersionMapper.selectOne(
                Wrappers.<AiAgentVersionEntity>lambdaQuery()
                        .select(
                                AiAgentVersionEntity::getId,
                                AiAgentVersionEntity::getTenantId,
                                AiAgentVersionEntity::getAgentId,
                                AiAgentVersionEntity::getSysPrompt,
                                AiAgentVersionEntity::getModelConfigId,
                                AiAgentVersionEntity::getCompactionConfigJson,
                                AiAgentVersionEntity::getWorkspaceConfigJson,
                                AiAgentVersionEntity::getPublishStatus
                        )
                        .eq(AiAgentVersionEntity::getTenantId, tenantId)
                        .eq(AiAgentVersionEntity::getAgentId, agentId)
                        .eq(AiAgentVersionEntity::getId, currentVersionId)
                        .eq(AiAgentVersionEntity::getPublishStatus, PUBLISHED)
                        .last("LIMIT 1")
        );

        if (agentVersion == null) {
            throw new AgentConfigException("Agent 当前版本不存在或未发布: " + currentVersionId);
        }

        return agentVersion;
    }

    private AiModelConfigEntity loadEnabledModelConfig(Long tenantId, Long modelConfigId) {
        if (modelConfigId == null) {
            throw new AgentConfigException("Agent 版本未配置模型");
        }

        AiModelConfigEntity modelConfig = modelConfigMapper.selectOne(
                Wrappers.<AiModelConfigEntity>lambdaQuery()
                        .select(
                                AiModelConfigEntity::getId,
                                AiModelConfigEntity::getTenantId,
                                AiModelConfigEntity::getCredentialId,
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

    private void validateCredentialIfNecessary(Long tenantId, AiModelConfigEntity modelConfig) {
        Long credentialId = modelConfig.getCredentialId();

        // Ollama、本地模型或者平台内置模型，credential_id 可以为空
        if (credentialId == null) {
            return;
        }

        AiLlmCredentialEntity credential = llmCredentialMapper.selectOne(
                Wrappers.<AiLlmCredentialEntity>lambdaQuery()
                        .select(
                                AiLlmCredentialEntity::getId,
                                AiLlmCredentialEntity::getTenantId,
                                AiLlmCredentialEntity::getProvider,
                                AiLlmCredentialEntity::getStatus
                        )
                        .eq(AiLlmCredentialEntity::getTenantId, tenantId)
                        .eq(AiLlmCredentialEntity::getId, credentialId)
                        .eq(AiLlmCredentialEntity::getStatus, ENABLED)
                        .last("LIMIT 1")
        );

        if (credential == null) {
            throw new AgentConfigException("模型凭证不存在或已停用: " + credentialId);
        }

        if (!modelConfig.getProvider().equalsIgnoreCase(credential.getProvider())) {
            throw new AgentConfigException(
                    "模型配置 provider 与凭证 provider 不一致，modelConfigId="
                            + modelConfig.getId()
                            + ", credentialId="
                            + credentialId
            );
        }
    }

    private String defaultWorkspacePath(String tenantCode, Long agentId, Long versionId) {
        return "/tmp/agentscope/workspaces/"
                + tenantCode
                + "/agent-"
                + agentId
                + "/version-"
                + versionId;
    }

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