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
 * 子Agent定义表
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_subagent")
public class AiSubagentEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 子Agent定义ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 子Agent唯一标识，例如 remote-researcher
     */
    @TableField("subagent_code")
    private String subagentCode;

    /**
     * 子Agent显示名称
     */
    @TableField("subagent_name")
    private String subagentName;

    /**
     * 能力描述，供主Agent判断何时委派
     */
    @TableField("description")
    private String description;

    /**
     * 来源类型：1.平台Agent 2.远程Agent Protocol
     */
    @TableField("source_type")
    private Byte sourceType;

    /**
     * 平台内部子Agent ID，source_type=1时使用，关联ai_agent.id
     */
    @TableField("local_agent_id")
    private Long localAgentId;

    /**
     * 远程Agent Protocol服务基础URL，source_type=2时使用
     */
    @TableField("remote_url")
    private String remoteUrl;

    /**
     * 远程协议类型：1.Agent Protocol，预留2.A2A
     */
    @TableField("protocol_type")
    private Byte protocolType;

    /**
     * 是否启用：0.否 1.是
     */
    @TableField("enabled")
    private Byte enabled;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
