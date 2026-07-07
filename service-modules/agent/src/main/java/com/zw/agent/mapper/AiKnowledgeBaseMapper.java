package com.zw.agent.mapper;

import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Mapper
public interface AiKnowledgeBaseMapper extends BaseMapper<AiKnowledgeBaseEntity> {

}
