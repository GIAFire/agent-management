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
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent")
public class AiAgentEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Agent 业务唯一编码，例如 customer-service-agent
     */
    @TableField("agent_key")
    private String agentKey;

    /**
     * Agent 显示名称
     */
    @TableField("agent_name")
    private String agentName;

    /**
     * Agent 描述
     */
    @TableField("description")
    private String description;

    /**
     * Agent 类型：HARNESS 或 REACT；平台默认 HARNESS
     */
    @TableField("agent_type")
    private String agentType;

    /**
     * 当前发布版本ID，关联 agent_config_id.id
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 状态：1启用，0停用，2草稿
     */
    @TableField("status")
    private Byte status;
}
