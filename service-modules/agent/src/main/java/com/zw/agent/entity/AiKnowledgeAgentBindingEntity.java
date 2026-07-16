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
 * Agent知识库绑定表：控制Agent配置可访问的知识库及检索参数
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_agent_binding")
public class AiKnowledgeAgentBindingEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent知识库绑定主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置版本ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 知识库ID，关联ai_knowledge_base.id
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * RAG模式：NONE不启用、GENERIC调用前自动检索、AGENTIC由Agent通过工具主动检索
     */
    @TableField("rag_mode")
    private String ragMode;

    /**
     * 检索返回结果数量
     */
    @TableField("retrieve_top_k")
    private Integer retrieveTopK;

    /**
     * 相似度阈值，低于该分数不返回
     */
    @TableField("score_threshold")
    private BigDecimal scoreThreshold;

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
}
