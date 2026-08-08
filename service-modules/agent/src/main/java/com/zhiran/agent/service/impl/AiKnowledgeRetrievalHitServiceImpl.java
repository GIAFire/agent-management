package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiKnowledgeRetrievalHitEntity;
import com.zhiran.agent.mapper.AiKnowledgeRetrievalHitMapper;
import com.zhiran.agent.service.AiKnowledgeRetrievalHitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 知识库检索命中明细表：记录每次检索命中的文档切片、分数和引用信息 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-06
 */
@Service
public class AiKnowledgeRetrievalHitServiceImpl extends ServiceImpl<AiKnowledgeRetrievalHitMapper, AiKnowledgeRetrievalHitEntity> implements AiKnowledgeRetrievalHitService {

}
