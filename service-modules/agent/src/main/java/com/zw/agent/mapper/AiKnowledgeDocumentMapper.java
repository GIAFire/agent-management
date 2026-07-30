package com.zw.agent.mapper;

import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 知识库文档表：记录平台文档与外部RAG/向量库文档的映射关系 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocumentEntity> {

    @Select("""
            SELECT *
            FROM ai_knowledge_document
            WHERE id = #{documentId}
              AND deleted = 0
            FOR UPDATE
            """)
    AiKnowledgeDocumentEntity selectByIdForUpdate(
            @Param("documentId") Long documentId
    );
}
