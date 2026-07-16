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
 * Agent与Tool绑定表：定义某个Agent启用了哪些工具
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_tool")
public class AiAgentToolEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent与Tool绑定主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * AgentID，关联 ai_agent.id
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置ID，关联 ai_agent_config.id
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 工具权限ID，关联 ai_tool_role_permission.id
     */
    @TableField("tool_info_config_id")
    private Long toolInfoConfigId;

    /**
     * 工具函数名，必须与AgentScope注册到Toolkit中的工具名一致
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 工具中文名，可用于前端展示或给Agent暴露不同名称
     */
    @TableField("tool_alias")
    private String toolAlias;

    /**
     * 工具描述
     */
    @TableField("tool_description")
    private String toolDescription;

    /**
     * 工具分组，例如database、workspace、rag、sandbox、business
     */
    @TableField("tool_group")
    private String toolGroup;

    /**
     * 状态：1正常，0删除或停用
     */
    @TableField("status")
    private Byte status;
}
