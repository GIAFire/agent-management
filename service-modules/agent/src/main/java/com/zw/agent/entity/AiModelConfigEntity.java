package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("ai_model_config")
public class AiModelConfigEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 模型配置主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型配置主键ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 模型供应商
     */
    @TableField("provider")
    private String provider;

    /**
     * apiUrl
     */
    @TableField("base_URL")
    private String baseURL;

    /**
     * apiKey密文
     */
    @TableField("api_key")
    private String apiKey;


    /**
     * 真实模型名称，例如 qwen-plus、gpt-4.1、claude-sonnet-4-5
     */
    @TableField("model_name")
    private String modelName;


    /**
     * 是否流式输出:1是,0否
     */
    @TableField("is_stream")
    private Integer isStream;

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
     * 模型调用超时时间，单位毫秒
     */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /**
     * 模型调用最大重试次数
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
    private Byte status;

}
