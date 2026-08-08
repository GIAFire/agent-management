package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiSubagentEntity;
import com.zhiran.agent.entity.DTO.SubagentHeaderDTO;
import com.zhiran.agent.mapper.AiSubagentMapper;
import com.zhiran.agent.service.AiSubagentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-11
 */
@RequiredArgsConstructor
@Service
public class AiSubagentServiceImpl extends ServiceImpl<AiSubagentMapper, AiSubagentEntity> implements AiSubagentService {

    private final AiSubagentMapper subagentMapper;

    @Override
    public List<SubagentHeaderDTO> subAgentList(Long agentId, Long tenantId) {
        return subagentMapper.subAgentList(agentId, tenantId);
    }
}
