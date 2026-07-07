package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 知识库检索命中明细表：记录每次检索命中的文档切片、分数和引用信息
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_retrieval_hit")
public class AiKnowledgeRetrievalHitEntity extends BaseEntity {

    /**
     * 检索命中明细主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 检索日志ID，关联ai_knowledge_retrieval_log.id
     */
    @TableField("retrieval_log_id")
    private Long retrievalLogId;

    /**
     * 知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 平台切片ID
     */
    @TableField("chunk_id")
    private Long chunkId;

    /**
     * 外部切片ID，例如RAGFlow chunk_id、向量库point_id
     */
    @TableField("external_chunk_id")
    private String externalChunkId;

    /**
     * 召回排名，从1开始
     */
    @TableField("rank_no")
    private Integer rankNo;

    /**
     * 最终相似度分数
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 向量相似度分数
     */
    @TableField("vector_score")
    private BigDecimal vectorScore;

    /**
     * 关键词或BM25分数
     */
    @TableField("keyword_score")
    private BigDecimal keywordScore;

    /**
     * rerank分数
     */
    @TableField("rerank_score")
    private BigDecimal rerankScore;

    /**
     * 命中文档名称
     */
    @TableField("document_name")
    private String documentName;

    /**
     * 命中切片内容快照，用于审计和回放
     */
    @TableField("chunk_content")
    private String chunkContent;

    /**
     * 来源引用，例如页码、章节、URL、文件路径
     */
    @TableField("source_ref")
    private String sourceRef;

    /**
     * 是否最终注入模型上下文：1是，0否
     */
    @TableField("used_in_prompt")
    private Byte usedInPrompt;
}
