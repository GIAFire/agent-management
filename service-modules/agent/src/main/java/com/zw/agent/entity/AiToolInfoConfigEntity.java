package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 全局工具配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_tool_info_config")
public class AiToolInfoConfigEntity extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 ai_agent.id
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 工具显示名称
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 所属组
     */
    @TableField("group_id")
    private String groupId;

    /**
     * 权限码
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 工具显示名称
     */
    @TableField("tool_name_explain")
    private String toolNameExplain;

    /**
     * 工具描述，给模型看的能力说明
     */
    @TableField("description")
    private String description;

    /**
     * 类全限定名 + 方法名 + 参数类型列表
     */
    @TableField("tool_key")
    private String toolKey;

    /**
     * sha256签名,用于判断tool是否修改
     */
    @TableField("signature_hash")
    private String signatureHash;

    /**
     * 工具类型：JAVA_BEAN、MCP、HTTP、RPC、SQL 等
     */
    @TableField("tool_type")
    private String toolType;

    /**
     * Spring Bean 名称，例如 orderTools
     */
    @TableField("bean_name")
    private String beanName;

    /**
     * 工具所在 Java 类全限定名
     */
    @TableField("class_name")
    private String className;

    /**
     * 工具方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 工具入参 JSON Schema，可选，通常可由 ToolBase 生成
     */
    @TableField("input_schema")
    private String inputSchema;

    /**
     * 工具出参 Schema，可选
     */
    @TableField("output_schema")
    private String outputSchema;

    /**
     * 是否只读工具（默认 true）：1是，0否
     */
    @TableField("read_only")
    private Boolean readOnly;

    /**
     * 是否并发调用（默认 false）：1是，0否
     */
    @TableField("concurrency")
    private Boolean concurrency;

    /**
     * 是否在调用时注入 AgentState 作为额外参数（默认 false）：1是，0否
     */
    @TableField("state_injected")
    private Boolean stateInjected;

    /**
     * 风险等级：LOW、MEDIUM、HIGH
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 是否启用：1启用，0停用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 工具调用超时时间，单位毫秒
     */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /**
     * 最大重试次数
     */
    @TableField("max_retries")
    private Integer maxRetries;

    /**
     * 默认工具组编码
     */
    @TableField("match_rule")
    private String matchRule;

    /**
     * 默认工具组编码
     */
    @TableField("default_group_code")
    private String defaultGroupCode;

}
