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
 * Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_plan_task")
public class AiAgentPlanTaskEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计划任务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 计划ID，关联ai_agent_plan.id
     */
    @TableField("plan_id")
    private Long planId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 最近一次更新该任务的运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 任务序号
     */
    @TableField("task_index")
    private Integer taskIndex;

    /**
     * 任务标题或任务描述
     */
    @TableField("subject")
    private String subject;

    /**
     * 任务详细说明
     */
    @TableField("detail")
    private String detail;

    /**
     * 任务状态：PENDING待执行，IN_PROGRESS执行中，COMPLETED已完成，BLOCKED阻塞，FAILED失败，CANCELLED取消
     */
    @TableField("state")
    private String state;

    /**
     * 任务负责人：主Agent、子Agent名称或用户
     */
    @TableField("owner")
    private String owner;

    /**
     * 该任务阻塞哪些任务ID或序号
     */
    @TableField("blocks_json")
    private String blocksJson;

    /**
     * 该任务被哪些任务阻塞
     */
    @TableField("blocked_by_json")
    private String blockedByJson;

    /**
     * 完成证据，例如文件ID、工具调用ID、结果摘要
     */
    @TableField("evidence_json")
    private String evidenceJson;

    /**
     * 任务开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 任务完成时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
