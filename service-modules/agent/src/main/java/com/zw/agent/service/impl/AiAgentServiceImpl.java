package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentConfigEntity;
import com.zw.agent.entity.AiAgentEntity;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import com.zw.agent.mapper.AiAgentConfigMapper;
import com.zw.agent.mapper.AiAgentMapper;
import com.zw.agent.service.AiAgentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;

/**
 * <p>
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@RequiredArgsConstructor
@Service
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgentEntity> implements AiAgentService {


    private final AiAgentMapper aiAgentMapper;
    private final AiAgentConfigMapper agentConfigMapper;
    @Override
    public AgentConfigDTO getAgentFullInfo(Long tenantId,Long agentId) {
        return aiAgentMapper.getAgentFullInfo(agentId, tenantId);
    }

    @Transient
    @Override
    public Boolean createAgent(AgentConfigDTO agentVO) {
        AiAgentEntity aiAgentEntity = new AiAgentEntity();
        BeanUtils.copyProperties(agentVO, aiAgentEntity);
        aiAgentMapper.insert(aiAgentEntity);
        AiAgentConfigEntity aiAgentConfigEntity = new AiAgentConfigEntity();
        BeanUtils.copyProperties(agentVO, aiAgentConfigEntity);
        agentConfigMapper.insert(aiAgentConfigEntity);
        return null;
    }
}
