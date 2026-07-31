package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.agent.constant.enumeration.ModelProtocol;
import com.zw.common.entity.BaseEntity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_model")
public class AiAgentModelEntity extends BaseEntity {


    /**
     * 模型配置主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户内唯一的模型配置名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 模型供应商显示名称
     */
    @TableField("provider_name")
    private String providerName;

    /**
     * 模型接口协议
     */
    @TableField("protocol")
    private ModelProtocol protocol;

    /**
     * apiUrl
     */
    @TableField("base_URL")
    private String baseURL;

    /**
     * API Key；当前阶段明文存储
     */
    @TableField("api_key")
    private String apiKey;


    /**
     * 模型介绍
     */
    @TableField("description")
    private String description;

    /**
     * 真实模型名称，例如 qwen-plus、gpt-4.1、claude-sonnet-4-5
     */
    @TableField("model_name")
    private String modelName;


    /**
     * 是否支持流式输出:1是,0否
     */
    @TableField("streaming")
    private Integer streaming;

    /**
     * 是否支持思考:1是,0否
     */
    @TableField("thinking")
    private Integer thinking;

    /**
     * 采样温度，控制随机性
     */
    @TableField("temperature")
    private BigDecimal temperature;

    /**
     * 核采样参数
     */
    @TableField("top_p")
    private BigDecimal topP;

    /**
     * 最大输出 token 数
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 单次逻辑模型调用超时时间，单位毫秒
     */
    @TableField("timeout_ms")
    private Long timeoutMs;

    /**
     * 思考token预算
     */
    @TableField("thinking_budget")
    private Integer thinkingBudget;

    /**
     * 最大尝试次数，包含首次请求
     */
    @TableField("max_attempts")
    private Integer maxAttempts;

    /**
     * 失败时兜底模型配置ID
     */
    @TableField("fallback_model_config_id")
    private Long fallbackModelConfigId;

    /**
     * 状态：1启用，0停用
     */
    @TableField("status")
    private Integer status;

}
