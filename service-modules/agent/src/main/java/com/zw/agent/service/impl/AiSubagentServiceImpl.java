package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSubagentEntity;
import com.zw.agent.mapper.AiSubagentMapper;
import com.zw.agent.service.AiSubagentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 子Agent定义表：保存可复用专家Agent的能力描述、模型、工具、知识库和安全配置 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@RequiredArgsConstructor
@Service
public class AiSubagentServiceImpl extends ServiceImpl<AiSubagentMapper, AiSubagentEntity> implements AiSubagentService {

    private final AiSubagentMapper subagentMapper;

    @Override
    public List<AiSubagentEntity> subAgentList(Long agentId) {
        return subagentMapper.subAgentList(agentId);
    }
}
