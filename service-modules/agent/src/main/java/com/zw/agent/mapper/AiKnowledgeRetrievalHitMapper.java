package com.zw.agent.mapper;

import com.zw.agent.entity.AiKnowledgeRetrievalHitEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 知识库检索命中明细表：记录每次检索命中的文档切片、分数和引用信息 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Mapper
public interface AiKnowledgeRetrievalHitMapper extends BaseMapper<AiKnowledgeRetrievalHitEntity> {

}
