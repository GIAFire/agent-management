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
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }
        AgentConfigDTO agentConfig = agentServiceImpl.getAgentFullInfo(agentId);

        try {
            String decrypt = AESUtil.decrypt(agentConfig.getApiKey());
            agentConfig.setApiKey(decrypt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return agentConfig;
    }
}