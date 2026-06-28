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
 * Agent权限规则表：定义某个工具在不同调用模式下允许、拒绝或询问
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_tool_role_permission")
public class AiToolRolePermissionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 权限规则主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 工具id
     */
    @TableField("tool_id")
    private Long toolId;

    /**
     * 角色id
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 角色id
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 工具名称，必须和AgentScope Tool名称一致
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 规则内容，传给Tool.matchRule使用；为空表示匹配该工具所有调用
     */
    @TableField("rule_content")
    private String ruleContent;

    /**
     * 规则行为：ALLOW允许，DENY拒绝，ASK询问，PASSTHROUGH交给后续规则
     */
    @TableField("behavior")
    private String behavior;

    /**
     * 规则来源：userSettings/projectSettings/session/suggested/admin
     */
    @TableField("source")
    private String source;

    /**
     * 规则说明，给管理员查看
     */
    @TableField("description")
    private String description;

    /**
     * 状态：1启用，0停用
     */
    @TableField("status")
    private Byte status;
}
