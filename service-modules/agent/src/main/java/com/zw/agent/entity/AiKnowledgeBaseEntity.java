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
 * 知识库表：平台知识库抽象层，兼容RAGFlow及不同向量库
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_base")
public class AiKnowledgeBaseEntity extends BaseEntity {

    /**
     * 知识库主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 知识库名称，例如产品文档库、售后政策库
     */
    @TableField("knowledge_name")
    private String knowledgeName;

    /**
     * 知识库说明，描述用途和内容范围
     */
    @TableField("description")
    private String description;

    /**
     * 向量存储配置ID，关联ai_knowledge_vector_config.id
     */
    @TableField("vector_store_id")
    private Long vectorStoreId;

    /**
     * 知识库后端类型：RAGFLOW/SELF_MANAGED/QDRANT/MILVUS/PGVECTOR/ELASTICSEARCH
     */
    @TableField("provider_type")
    private String providerType;

    /**
     * 外部知识库ID，例如RAGFlow dataset_id、Elasticsearch index、Qdrant collection
     */
    @TableField("external_dataset_id")
    private String externalDatasetId;

    /**
     * 切片策略：GENERAL/PARAGRAPH/TOKEN/QA/TABLE/PDF_LAYOUT/CUSTOM
     */
    @TableField("chunk_strategy")
    private String chunkStrategy;

    /**
     * 切片大小，单位可按字符或token，由实现决定
     */
    @TableField("chunk_size")
    private Integer chunkSize;

    /**
     * 切片重叠大小，用于保留上下文连续性
     */
    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    /**
     * 默认返回结果数量
     */
    @TableField("retrieve_top_k")
    private Integer retrieveTopK;

    /**
     * 默认相似度阈值
     */
    @TableField("score_threshold")
    private BigDecimal scoreThreshold;

    /**
     * 是否启用rerank：1启用，0关闭
     */
    @TableField("rerank_enabled")
    private Byte rerankEnabled;

    /**
     * 可见范围：PRIVATE私有、AGENT智能体、TENANT租户
     */
    @TableField("visibility")
    private String visibility;

    /**
     * 状态：1启用，0停用，2删除
     */
    @TableField("status")
    private Byte status;

    /**
     * 后端特定元信息，例如RAGFlow dataset配置、索引版本等
     */
    @TableField("provider_meta_json")
    private String providerMetaJson;
}
