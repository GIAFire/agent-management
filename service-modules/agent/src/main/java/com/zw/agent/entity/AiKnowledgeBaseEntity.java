package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zw.agent.constant.enumeration.ApiType;
import com.zw.common.entity.BaseEntity;

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
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 知识库名称，例如产品文档库、售后政策库
     */
    @TableField("knowledge_name")
    private String knowledgeName;

    /**
     * 知识库英文名称
     */
    @TableField("knowledge_code")
    private String knowledgeCode;

    /**
     * 集合名称，例如RAGFlow的dataset名称
     */
    @TableField("collection_name")
    private String collectionName;

    /**
     * 知识库说明，描述用途和内容范围
     */
    @TableField("description")
    private String description;

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
     * 是否启用rerank：1启用，0关闭
     */
    @TableField("rerank_enabled")
    private Byte rerankEnabled;

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

    /**
     * 后端类型:如openai、百炼、RAGFlow、Dify
     */
    @TableField("backend_store_type")
    private String backendStoreType;

    /**
     * 本地后端API类型:如openai、ollama
     */
    @TableField("api_type")
    private ApiType apiType;

    /**
     * RAG服务访问地址，例如RAGFlow地址、Qdrant地址
     */
    @TableField("endpoint")
    private String endpoint;

    /**
     * 向量库或RAG服务访问地址，例如RAGFlow地址、Qdrant地址
     */
    @TableField("endpoint_port")
    private Integer endpointPort;

    /**
     * 模型url
     */
    @TableField("model_url")
    private String modelUrl;

    /**
     * Embedding API Key。当前按产品决策直接保存，任何查询接口均不得返回。
     */
    @JsonIgnore
    @TableField("api_key")
    private String apiKey;

    /**
     * Embedding模型名称，例如 text-embedding-v3
     */
    @TableField("embedding_model_name")
    private String embeddingModelName;

    /**
     * 向量维度，例如768、1024、1536
     */
    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 距离度量：COSINE/IP/L2/BM25/HYBRID
     */
    @TableField("metric_type")
    private String metricType;

    /**
     * 默认返回结果数量
     */
    @TableField("top_k")
    private Integer topK;

    /**
     * 默认相似度阈值
     */
    @TableField("score_threshold")
    private BigDecimal scoreThreshold;

    /**
     * 后端扩展配置JSON，例如rerank配置、hybrid检索权重、RAGFlow参数
     */
    @TableField("config_json")
    private String configJson;
}
