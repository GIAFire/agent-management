package com.zw.agent.runtime;

import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.exception.AgentConfigException;
import com.zw.agent.service.impl.AiAgentServiceImpl;
import com.zw.common.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Agent配置查询服务类，负责从数据库中加载已发布的Agent运行时配置。
 * 通过租户编码、Agent ID等参数，级联查询租户、Agent定义、版本信息、模型配置等数据，
 * 构建完整的AgentRuntimeConfig对象供运行时使用。
 */
@RequiredArgsConstructor
@Service
public class AgentFullConfigService {


    private final ObjectMapper objectMapper;
    private final AiAgentServiceImpl agentServiceImpl;

    /**
     * 加载已发布的Agent运行时配置。
     * 该方法会依次验证并加载：启用的租户、启用的Agent定义、已发布的Agent版本、启用的模型配置，
     * 并从JSON配置中提取工作空间路径和消息压缩配置，最终构建完整的AgentRuntimeConfig对象。
     *
     * @param tenantId 租户编码，用于标识多租户环境中的具体租户
     * @param agentId Agent唯一标识ID，用于定位具体的Agent实例
     * @return 完整的Agent运行时配置对象，包含租户ID、Agent信息、模型配置、工作空间路径、压缩策略等
     * @throws AgentConfigException 当租户不存在或已停用、Agent不存在或未发布、模型配置无效时抛出异常
     */
    public AgentConfigDTO loadPublishedConfig(Long tenantId, Long agentId) {
        if (!StringUtils.hasText(tenantId.toString())) {
            throw new AgentConfigException("tenantId 不能为空");
        }
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }
        // 查询租户信息、Agent定义、Agent配置信息和模型配置
        AgentConfigDTO agentFullInfo = agentServiceImpl.getAgentFullInfo(tenantId, agentId);

        try {
            String decrypt = AESUtil.decrypt(agentFullInfo.getApiKey());
            agentFullInfo.setApiKey(decrypt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 从JSON配置中提取工作空间路径，若未配置则使用默认路径
        String workspacePath = readText(
                agentFullInfo.getWorkspaceConfigJson(),
                defaultWorkspacePath(tenantId, agentId, agentFullInfo.getAgentConfigId()),
                "workspacePath",
                "path"
        );
        agentFullInfo.setWorkspacePath(workspacePath);

        // 从JSON配置中提取消息压缩触发阈值和保留数量
        int compactionTriggerMessages = readInt(
                agentFullInfo.getCompactionConfigJson(),
                20,
                "triggerMessages",
                "compactionTriggerMessages"
        );
        agentFullInfo.setCompactionTriggerMessages(compactionTriggerMessages);

        int compactionKeepMessages = readInt(
                agentFullInfo.getCompactionConfigJson(),
                8,
                "keepMessages",
                "compactionKeepMessages"
        );
        agentFullInfo.setCompactionKeepMessages(compactionKeepMessages);

        return agentFullInfo;
    }


    /**
     * 生成默认的Agent工作空间路径。
     * 根据租户编码、Agent ID和版本ID构建文件系统路径，用于存储Agent的临时文件和运行时数据。
     *
     * @param tenantId 租户编码，用于隔离不同租户的工作空间
     * @param agentId Agent ID，用于区分不同的Agent
     * @param versionId 版本ID，用于区分不同的Agent版本
     * @return 默认的工作空间路径字符串，格式为 /tmp/agentscope/workspaces/{tenantId}/agent-{agentId}/version-{versionId}
     */
    private String defaultWorkspacePath(Long tenantId, Long agentId, Long versionId) {
        return "/tmp/agentscope/workspaces/"
                + tenantId
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
            JsonNode root = objectMapper.readTree(json);
            for (String fieldName : fieldNames) {
                JsonNode node = root.get(fieldName);
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
            JsonNode root = objectMapper.readTree(json);
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