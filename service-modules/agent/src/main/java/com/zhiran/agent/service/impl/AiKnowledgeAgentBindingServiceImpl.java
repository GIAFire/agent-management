package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.knowledge.KnowledgeConstants;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import com.zhiran.agent.mapper.AiKnowledgeAgentBindingMapper;
import com.zhiran.agent.mapper.AiKnowledgeBaseMapper;
import com.zhiran.agent.service.AiKnowledgeAgentBindingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Agent知识库绑定表：控制Agent配置可访问的知识库及检索参数 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-11
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeAgentBindingServiceImpl extends ServiceImpl<AiKnowledgeAgentBindingMapper, AiKnowledgeAgentBindingEntity> implements AiKnowledgeAgentBindingService {

    private final AiKnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AiKnowledgeAgentBindingEntity entity) {
        lockActiveKnowledgeBase(
                entity == null ? null : entity.getKnowledgeBaseId()
        );
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(AiKnowledgeAgentBindingEntity entity) {
        if (entity == null || entity.getId() == null) {
            throw new KnowledgeOperationException("知识库绑定ID不能为空");
        }
        AiKnowledgeAgentBindingEntity existing = getById(entity.getId());
        if (existing == null) {
            throw new KnowledgeOperationException("知识库绑定不存在");
        }
        Long knowledgeBaseId = entity.getKnowledgeBaseId() == null
                ? existing.getKnowledgeBaseId()
                : entity.getKnowledgeBaseId();
        Byte status = entity.getStatus() == null
                ? existing.getStatus()
                : entity.getStatus();
        if (status != null && status == KnowledgeConstants.ENABLED) {
            lockActiveKnowledgeBase(knowledgeBaseId);
        }
        return super.updateById(entity);
    }

    private void lockActiveKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new KnowledgeOperationException("知识库ID不能为空");
        }
        AiKnowledgeBaseEntity knowledgeBase =
                knowledgeBaseMapper.selectByIdForUpdate(knowledgeBaseId);
        if (knowledgeBase == null
                || knowledgeBase.getStatus() == null
                || knowledgeBase.getStatus() != KnowledgeConstants.ENABLED) {
            throw new KnowledgeOperationException("只能绑定启用中的知识库");
        }
    }
}
