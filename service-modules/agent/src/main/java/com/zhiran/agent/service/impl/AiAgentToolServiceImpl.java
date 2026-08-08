package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiAgentToolEntity;
import com.zhiran.agent.entity.DTO.AgentBindToolDTO;
import com.zhiran.agent.mapper.AiAgentToolMapper;
import com.zhiran.agent.service.AiAgentToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Agent与Tool绑定表：定义某个Agent启用了哪些工具 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-12
 */
@RequiredArgsConstructor
@Service
public class AiAgentToolServiceImpl extends ServiceImpl<AiAgentToolMapper, AiAgentToolEntity> implements AiAgentToolService {

    private final AiAgentToolMapper aiAgentToolMapper;

    @Override
    public List<AgentBindToolDTO> agentBindTools(
            Long agentId,
            Long agentConfigId,
            Long tenantId
    ) {
        return aiAgentToolMapper.agentBindTools(agentId, agentConfigId, tenantId);
    }
}
