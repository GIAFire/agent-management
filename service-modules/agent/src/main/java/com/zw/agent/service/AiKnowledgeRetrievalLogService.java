package com.zw.agent.service;

import com.zw.agent.entity.AiKnowledgeRetrievalLogEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 知识库检索日志表：记录每次RAG检索请求、结果、注入内容和审计信息 服务类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
public interface AiKnowledgeRetrievalLogService extends IService<AiKnowledgeRetrievalLogEntity> {

}
