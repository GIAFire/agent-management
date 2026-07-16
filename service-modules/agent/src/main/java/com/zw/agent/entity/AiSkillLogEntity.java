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
 * Skill使用日志表：记录Agent读取、加载、执行Skill的行为
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_skill_log")
public class AiSkillLogEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Skill使用日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * SkillID，可能从skillId解析失败时为空
     */
    @TableField("skill_id")
    private Long skillId;

    /**
     * AgentScope运行时skill-id，例如code-reviewer_workspace-namespaced
     */
    @TableField("skill_runtime_id")
    private String skillRuntimeId;

    /**
     * 操作类型：LIST_AVAILABLE_SKILLS，LOAD_SKILL，READ_REFERENCE，RUN_SCRIPT
     */
    @TableField("operation")
    private String operation;

    /**
     * 是否成功：1成功，0失败
     */
    @TableField("success")
    private Byte success;

    /**
     * 失败信息
     */
    @TableField("error_message")
    private String errorMessage;
}
