package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 子Agent运行实例表：记录父Agent创建的子Agent实例、会话、工作区和暴露状态
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_subagent_instance")
public class AiSubagentInstanceEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 子Agent运行实例ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 父Agent ID
     */
    @TableField("parent_agent_id")
    private Long parentAgentId;

    /**
     * 父Agent配置版本ID
     */
    @TableField("parent_agent_config_id")
    private Long parentAgentConfigId;

    /**
     * 父会话ID
     */
    @TableField("parent_session_id")
    private Long parentSessionId;

    /**
     * 触发创建的父运行ID
     */
    @TableField("parent_run_id")
    private Long parentRunId;

    /**
     * 子Agent定义ID
     */
    @TableField("subagent_id")
    private Long subagentId;

    /**
     * 子Agent编码
     */
    @TableField("subagent_key")
    private String subagentKey;

    /**
     * LLM或平台指定的人类可读标签
     */
    @TableField("label")
    private String label;

    /**
     * AgentScope返回的运行时实例句柄agent_key
     */
    @TableField("agent_key")
    private String agentKey;

    /**
     * 暴露给前端或Channel的subagentId
     */
    @TableField("subagent_external_id")
    private String subagentExternalId;

    /**
     * RuntimeContext.userId
     */
    @TableField("runtime_user_key")
    private String runtimeUserKey;

    /**
     * 子Agent运行时sessionKey
     */
    @TableField("runtime_session_key")
    private String runtimeSessionKey;

    /**
     * ISOLATED/SHARED
     */
    @TableField("workspace_mode")
    private String workspaceMode;

    /**
     * 子Agent实际工作区路径
     */
    @TableField("workspace_path")
    private String workspacePath;

    /**
     * 是否已暴露给用户直接交互
     */
    @TableField("expose_to_user")
    private Byte exposeToUser;

    /**
     * 是否持久会话
     */
    @TableField("persist_session")
    private Byte persistSession;

    /**
     * RUNNING/IDLE/COMPLETED/FAILED/CANCELLED/EXPIRED
     */
    @TableField("status")
    private String status;

    /**
     * 启动时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 最后活跃时间
     */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    /**
     * 结束时间
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
}
