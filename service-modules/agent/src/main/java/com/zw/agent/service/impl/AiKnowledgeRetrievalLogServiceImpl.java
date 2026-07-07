package com.zw.agent.service.impl;

import com.zw.agent.entity.AiKnowledgeRetrievalLogEntity;
import com.zw.agent.mapper.AiKnowledgeRetrievalLogMapper;
import com.zw.agent.service.AiKnowledgeRetrievalLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 知识库检索日志表：记录每次RAG检索请求、结果、注入内容和审计信息 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Service
public class AiKnowledgeRetrievalLogServiceImpl extends ServiceImpl<AiKnowledgeRetrievalLogMapper, AiKnowledgeRetrievalLogEntity> implements AiKnowledgeRetrievalLogService {

}
