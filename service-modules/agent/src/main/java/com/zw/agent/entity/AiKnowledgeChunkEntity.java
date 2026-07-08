package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 知识切片表：记录文档切片内容、向量ID及引用元信息
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunkEntity extends BaseEntity {

    /**
     * 知识切片主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 知识库ID，关联ai_knowledge_base.id
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 文档ID，关联ai_knowledge_document.id
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 切片序号，从0或1开始
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 切片唯一标识，可用于幂等同步
     */
    @TableField("chunk_uid")
    private String chunkUid;

    /**
     * 外部切片ID，例如RAGFlow chunk_id、向量库point_id
     */
    @TableField("external_chunk_id")
    private String externalChunkId;

    /**
     * 切片文本内容，用于审计、引用展示和回源
     */
    @TableField("content")
    private String content;

    /**
     * 切片内容哈希，用于去重和变更检测
     */
    @TableField("content_hash")
    private String contentHash;

    /**
     * 切片内容类型：TEXT/TABLE/IMAGE/OCR/QA/CODE
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 页码，适用于PDF/DOCX等文档
     */
    @TableField("page_no")
    private Integer pageNo;

    /**
     * 章节标题或段落标题
     */
    @TableField("section_title")
    private String sectionTitle;

    /**
     * 切片在原文中的开始位置
     */
    @TableField("start_offset")
    private Integer startOffset;

    /**
     * 切片在原文中的结束位置
     */
    @TableField("end_offset")
    private Integer endOffset;

    /**
     * 切片估算token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 向量存储配置ID，关联ai_knowledge_backend_config.id
     */
    @TableField("vector_store_id")
    private Long vectorStoreId;

    /**
     * 向量库中的向量ID或point ID
     */
    @TableField("vector_id")
    private String vectorId;

    /**
     * Embedding模型配置ID
     */
    @TableField("embedding_model_config_id")
    private Long embeddingModelConfigId;

    /**
     * 向量维度
     */
    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 切片元信息JSON，例如表格结构、图片坐标、页码、标题路径
     */
    @TableField("metadata_json")
    private String metadataJson;

    /**
     * 状态：1启用，0禁用，2删除
     */
    @TableField("status")
    private Byte status;
}
