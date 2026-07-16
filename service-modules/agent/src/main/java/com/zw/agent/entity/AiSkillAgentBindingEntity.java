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
 * Agent与Skill绑定表：定义某个Agent配置版本安装哪些Skill以及安装作用域
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_skill_agent_binding")
public class AiSkillAgentBindingEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent与Skill绑定ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * AgentID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * SkillID
     */
    @TableField("skill_id")
    private Long skillId;

    /**
     * 用户级Skill所属用户ID；非USER安装为空
     */
    @TableField("install_user_id")
    private Long installUserId;

    /**
     * 加载模式：DYNAMIC每轮动态合并，BUILD_ONCE构建时合并一次
     */
    @TableField("load_mode")
    private String loadMode;

    /**
     * 同名覆盖策略：ALLOW_OVERRIDE允许高优先级覆盖，DENY_OVERRIDE禁止覆盖
     */
    @TableField("override_policy")
    private String overridePolicy;
}
