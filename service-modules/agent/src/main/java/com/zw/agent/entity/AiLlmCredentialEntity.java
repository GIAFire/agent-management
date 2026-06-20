package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.entity.BaseEntity;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 大模型凭证表：保存每个租户自己的模型供应商鉴权信息
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_llm_credential")
public class AiLlmCredentialEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 模型凭证主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型供应商：dashscope/openai/anthropic/gemini/ollama等
     */
    @TableField("provider")
    private String provider;

    /**
     * 凭证名称，给管理员识别用
     */
    @TableField("credential_name")
    private String credentialName;

    /**
     * 兼容 OpenAI 协议时的自定义 baseUrl
     */
    @TableField("base_url")
    private String baseUrl;

    /**
     * 加密后的 API Key，禁止明文存储
     */
    @TableField("api_key_cipher")
    private String apiKeyCipher;

    /**
     * 状态：1启用，0停用
     */
    @TableField("status")
    private Byte status;
}
