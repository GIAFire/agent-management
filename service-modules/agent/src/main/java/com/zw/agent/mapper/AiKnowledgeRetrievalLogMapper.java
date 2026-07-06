package com.zw.agent.mapper;

import com.zw.agent.entity.AiKnowledgeRetrievalLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 知识库检索日志表：记录每次RAG检索请求、结果、注入内容和审计信息 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Mapper
public interface AiKnowledgeRetrievalLogMapper extends BaseMapper<AiKnowledgeRetrievalLogEntity> {

}
