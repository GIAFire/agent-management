package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class KnowledgeBaseDTO {

    private Long id;

    /**
     * 知识库后端id（关联后端配置，但现已合并，该字段保留冗余）
     */
    private Long knowledgeBackendId;

    /**
     * 用户ID，用于用户隔离
     */
    private Long userId;

    /**
     * 租户ID，用于多租户隔离
     */
    private Long tenantId;

    /**
     * 知识库名称，例如产品文档库、售后政策库
     */
    private String knowledgeName;

    /**
     * 集合名称（向量库中的collection）
     */
    private String collectionName;

    /**
     * 知识库说明，描述用途和内容范围
     */
    private String description;

    /**
     * 切片策略：GENERAL/PARAGRAPH/TOKEN/QA/TABLE/PDF_LAYOUT/CUSTOM
     */
    private String chunkStrategy;

    /**
     * 切片大小，单位可按字符或token
     */
    private Integer chunkSize;

    /**
     * 切片重叠大小，用于保留上下文连续性
     */
    private Integer chunkOverlap;

    /**
     * 是否启用rerank：1启用，0关闭
     */
    private Boolean rerankEnabled;

    /**
     * 状态：1启用，0停用，2删除
     */
    private Integer status;

    /**
     * 后端特定元信息，例如RAGFlow dataset配置、索引版本等
     */
    private Map<String, Object> providerMetaJson;

    /**
     * 创建人用户ID
     */
    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本
     */
    private Integer version;

    /**
     * 删除标记：0未删, 1删除
     */
    private Integer deleted;

    /**
     * 更新人用户ID
     */
    private Long updateBy;

    // ==================== 后端配置信息（原 ai_knowledge_backend_config 字段） ====================

    /**
     * 后端类型：如本地、百炼、RAGFlow、Dify
     */
    private String backendStoreType;

    /**
     * 本地后端API类型：如openai、ollama
     */
    private String apiType;

    /**
     * RAG服务访问地址，例如RAGFlow地址、api_url
     */
    private String endpoint;

    /**
     * 向量库或RAG服务访问端口号
     */
    private Integer endpointPort;

    /**
     * 向量模型url
     */
    private String modelUrl;

    /**
     * API Key引用，不建议直接存明文，可存密钥管理系统引用
     */
    private String apiKeyRef;

    /**
     * Embedding模型名称，例如 text-embedding-v3
     */
    private String embeddingModelName;

    /**
     * 向量维度，例如768、1024、1536
     */
    private Integer embeddingDimension;

    /**
     * 距离度量：COSINE/IP/L2/BM25/HYBRID
     */
    private String metricType;

    /**
     * 检索返回结果数量，默认5
     */
    private Integer topK;

    /**
     * 相似度阈值，低于该分数不返回
     */
    private BigDecimal scoreThreshold;

    /**
     * 后端扩展配置JSON，例如rerank配置、hybrid检索权重、RAGFlow参数
     */
    private Map<String, Object> configJson;
}
