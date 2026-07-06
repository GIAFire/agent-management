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
 * 知识库检索日志表：记录每次RAG检索请求、结果、注入内容和审计信息
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_retrieval_log")
public class AiKnowledgeRetrievalLogEntity extends BaseEntity {

    /**
     * 知识库检索日志主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 触发检索的用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 触发检索的Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置版本ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * Agent运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 工具调用ID，AGENTIC模式下记录retrieve_knowledge的toolCallId
     */
    @TableField("tool_call_id")
    private String toolCallId;

    /**
     * 本次检索查询文本
     */
    @TableField("query_text")
    private String queryText;

    /**
     * 本次检索模式：GENERIC/AGENTIC
     */
    @TableField("rag_mode")
    private String ragMode;

    /**
     * 向量存储配置ID
     */
    @TableField("vector_store_id")
    private Long vectorStoreId;

    /**
     * 检索后端类型：RAGFLOW/QDRANT/MILVUS/PGVECTOR/ELASTICSEARCH/MYSQL_FULLTEXT
     */
    @TableField("provider_type")
    private String providerType;

    /**
     * 检索返回结果数量
     */
    @TableField("retrieve_top_k")
    private Integer retrieveTopK;

    /**
     * 相似度阈值
     */
    @TableField("score_threshold")
    private BigDecimal scoreThreshold;

    /**
     * 是否启用rerank：1启用，0关闭
     */
    @TableField("rerank_enabled")
    private Byte rerankEnabled;

    /**
     * 实际召回结果数量
     */
    @TableField("retrieved_count")
    private Integer retrievedCount;

    /**
     * 发送给检索后端的请求JSON
     */
    @TableField("request_json")
    private String requestJson;

    /**
     * 检索后端返回的原始响应JSON
     */
    @TableField("response_json")
    private String responseJson;

    /**
     * 最终注入给模型或返回给工具的文本内容
     */
    @TableField("injected_text")
    private String injectedText;

    /**
     * 是否检索成功：1成功，0失败
     */
    @TableField("success")
    private Byte success;

    /**
     * 检索失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 检索耗时，单位毫秒
     */
    @TableField("duration_ms")
    private Long durationMs;
}
