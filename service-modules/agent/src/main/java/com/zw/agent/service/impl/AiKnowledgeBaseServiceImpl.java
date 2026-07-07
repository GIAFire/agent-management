package com.zw.agent.service.impl;

import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.mapper.AiKnowledgeBaseMapper;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Service
public class AiKnowledgeBaseServiceImpl extends ServiceImpl<AiKnowledgeBaseMapper, AiKnowledgeBaseEntity> implements AiKnowledgeBaseService {

}
