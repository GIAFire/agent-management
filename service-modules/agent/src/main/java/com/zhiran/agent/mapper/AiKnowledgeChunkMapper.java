package com.zhiran.agent.mapper;

import com.zhiran.agent.entity.AiKnowledgeChunkEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    @InterceptorIgnore(tenantLine = "true")
    List<AiKnowledgeChunkEntity> selectReadyChunks(
            @Param("tenantId") Long tenantId,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("chunkIds") Collection<Long> chunkIds
    );

    @InterceptorIgnore(tenantLine = "true")
    int physicalDeleteByDocument(
            @Param("tenantId") Long tenantId,
            @Param("documentId") Long documentId
    );

    @InterceptorIgnore(tenantLine = "true")
    int physicalDeleteByKnowledgeBase(
            @Param("tenantId") Long tenantId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );
}
