package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_run")
public class AiAgentRunEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 运行主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 ai_agent_session.id
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 关联 ai_agent_definition.id
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 关联 ai_agent_config.id
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 触发本次运行的用户消息ID
     */
    @TableField("input_message_id")
    private Long inputMessageId;

    /**
     * 触发本次运行的AI消息ID
     */
    @TableField("output_message_id")
    private Long outputMessageId;

    /**
     * 运行状态：RUNNING/SUCCESS/FAILED/CANCELLED
     */
    @TableField("status")
    private String status;

    /**
     * 错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;
}
