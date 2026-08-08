package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.mapper.AiKnowledgeBaseMapper;
import com.zhiran.agent.service.AiKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-06
 */
@RequiredArgsConstructor
@Service
public class AiKnowledgeBaseServiceImpl extends ServiceImpl<AiKnowledgeBaseMapper, AiKnowledgeBaseEntity> implements AiKnowledgeBaseService {
    private final AiKnowledgeBaseMapper aiKnowledgeBaseMapper;

    @Override
    public List<AiKnowledgeBaseEntity> getAgentBindKnowledge(Long agentId, Long agentConfigId, Long tenantId) {
        return aiKnowledgeBaseMapper.getAgentBindKnowledge(agentId, agentConfigId, tenantId);
    }
}
