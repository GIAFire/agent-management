package com.zhiran.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiran.agent.entity.AiAgentEntity;
import com.zhiran.agent.entity.DTO.AgentConfigDTO;
import com.zhiran.agent.exception.AgentConfigException;
import com.zhiran.agent.mapper.AiAgentMapper;
import com.zhiran.agent.service.AiAgentService;
import com.zhiran.common.context.UserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl
        extends ServiceImpl<AiAgentMapper, AiAgentEntity>
        implements AiAgentService {

    private final AiAgentMapper agentMapper;

    @Override
    public AgentConfigDTO getAgentConfigById(Long agentId, UserInfo userInfo) {
        return getAgentConfigById(agentId, null, userInfo);
    }

    @Override
    public AgentConfigDTO getAgentConfigById(
            Long agentId,
            Long agentConfigId,
            UserInfo userInfo
    ) {
        if (agentId == null) {
            throw new AgentConfigException("agentId 不能为空");
        }
        if (userInfo == null || userInfo.getTenantId() == null) {
            throw new AgentConfigException("租户信息不能为空");
        }
        AgentConfigDTO config = agentMapper.getAgentConfigById(
                agentId, userInfo.getTenantId(), agentConfigId);
        if (config == null) {
            throw new AgentConfigException("智能体不存在、已删除或配置不可用");
        }
        if (config.getModelId() == null
                || config.getProtocol() == null
                || !Integer.valueOf(1).equals(config.getModelStatus())
                || !Integer.valueOf(0).equals(config.getModelDeleted())) {
            throw new AgentConfigException("智能体未绑定可用模型，请先完成模型配置");
        }
        return config;
    }

    @Override
    public List<AiAgentEntity> subAgentList(Long agentId, Long tenantId) {
        return agentMapper.subAgentList(agentId, tenantId);
    }
}
