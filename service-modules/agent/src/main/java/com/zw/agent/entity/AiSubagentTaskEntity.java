package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_subagent_task")
public class AiSubagentTaskEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 子Agent任务ID
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
     * 父Agent运行ID
     */
    @TableField("parent_run_id")
    private Long parentRunId;

    /**
     * 子Agent实例ID
     */
    @TableField("subagent_instance_id")
    private Long subagentInstanceId;

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
     * AgentScope后台任务ID，timeout_seconds=0时返回
     */
    @TableField("task_id")
    private String taskId;

    /**
     * SYNC同步，BACKGROUND后台
     */
    @TableField("task_mode")
    private String taskMode;

    /**
     * 委派给子Agent的任务内容
     */
    @TableField("task_input")
    private String taskInput;

    /**
     * 子Agent最终结果摘要
     */
    @TableField("task_result")
    private String taskResult;

    /**
     * RUNNING/COMPLETED/FAILED/CANCELLED/TIMEOUT
     */
    @TableField("status")
    private String status;

    /**
     * 任务超时时间
     */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /**
     * 子Agent token用量
     */
    @TableField("token_usage_json")
    private String tokenUsageJson;

    /**
     * 估算成本
     */
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 耗时毫秒
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
}
