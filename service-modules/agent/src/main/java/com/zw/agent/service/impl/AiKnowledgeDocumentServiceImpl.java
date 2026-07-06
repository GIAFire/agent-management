package com.zw.agent.service.impl;

import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.mapper.AiKnowledgeDocumentMapper;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 知识库文档表：记录平台文档与外部RAG/向量库文档的映射关系 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Service
public class AiKnowledgeDocumentServiceImpl extends ServiceImpl<AiKnowledgeDocumentMapper, AiKnowledgeDocumentEntity> implements AiKnowledgeDocumentService {

}
