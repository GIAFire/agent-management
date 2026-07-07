package com.zw.agent.mapper;

import com.zw.agent.entity.AiKnowledgeChunkEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 知识切片表：记录文档切片内容、向量ID及引用元信息 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunkEntity> {

}
