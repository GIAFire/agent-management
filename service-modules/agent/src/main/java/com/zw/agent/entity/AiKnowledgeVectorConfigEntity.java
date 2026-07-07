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
 * 知识库向量存储配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_vector_config")
public class AiKnowledgeVectorConfigEntity extends BaseEntity {

    /**
     * 向量存储配置主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 向量存储名称，例如默认Qdrant、RAGFlow生产库
     */
    @TableField("store_name")
    private String storeName;

    /**
     * 向量存储类型：RAGFLOW/QDRANT/MILVUS/PGVECTOR/ELASTICSEARCH/OPENSEARCH/MYSQL_FULLTEXT
     */
    @TableField("store_type")
    private String storeType;

    /**
     * 向量库或RAG服务访问地址，例如RAGFlow地址、Qdrant地址
     */
    @TableField("endpoint")
    private String endpoint;

    /**
     * API Key引用，不建议直接存明文，可存密钥管理系统引用
     */
    @TableField("api_key_ref")
    private String apiKeyRef;

    /**
     * 数据库名或命名空间，例如Milvus database、PG database
     */
    @TableField("database_name")
    private String databaseName;

    /**
     * 集合名、索引名或RAGFlow dataset命名空间
     */
    @TableField("collection_name")
    private String collectionName;

    /**
     * Embedding模型配置ID，关联你的模型配置表
     */
    @TableField("embedding_model_config_id")
    private Long embeddingModelConfigId;

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
    @TableField("distance_metric")
    private String distanceMetric;

    /**
     * 后端特定配置JSON，例如rerank配置、hybrid检索权重、RAGFlow参数
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 状态：1启用，0停用，2删除
     */
    @TableField("status")
    private Byte status;
}
