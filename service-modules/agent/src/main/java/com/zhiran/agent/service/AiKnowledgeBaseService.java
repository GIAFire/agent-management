package com.zhiran.agent.service;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
public interface AiKnowledgeBaseService extends IService<AiKnowledgeBaseEntity> {

    List<AiKnowledgeBaseEntity> getAgentBindKnowledge(Long agentId, Long agentConfigId, Long tenantId);
}
