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
 * Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_plan_op_log")
public class AiAgentPlanOpLogEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计划操作日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID，Agent自动操作时可为空
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置版本ID
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
     * 计划ID
     */
    @TableField("plan_id")
    private Long planId;

    /**
     * 操作类型：PLAN_ENTER，PLAN_WRITE，PLAN_EXIT_REQUEST，PLAN_APPROVE，PLAN_REJECT，TODO_WRITE，PLAN_STATUS_CHANGE
     */
    @TableField("op_type")
    private String opType;

    /**
     * AgentScope工具调用ID
     */
    @TableField("tool_call_id")
    private String toolCallId;

    /**
     * 操作前计划状态
     */
    @TableField("before_status")
    private String beforeStatus;

    /**
     * 操作后计划状态
     */
    @TableField("after_status")
    private String afterStatus;

    /**
     * 操作原始载荷，例如工具入参、任务列表、审批信息
     */
    @TableField("payload_json")
    private String payloadJson;

    /**
     * 是否成功：1成功，0失败
     */
    @TableField("success")
    private Byte success;

    /**
     * 失败原因
     */
    @TableField("error_message")
    private String errorMessage;
}
