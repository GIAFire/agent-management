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
 * Agent计划表：保存Plan Mode生成的计划元数据、内容快照、审批和执行状态
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_plan")
public class AiAgentPlanEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计划ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，表示是谁触发了该计划
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
     * 创建或更新该计划的运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 计划编号，平台内可读编号，例如PLAN-20260708-0001
     */
    @TableField("plan_no")
    private String planNo;

    /**
     * 计划标题
     */
    @TableField("title")
    private String title;

    /**
     * 用户原始目标或Agent总结后的目标
     */
    @TableField("goal")
    private String goal;

    /**
     * 计划状态：DRAFT草稿，WAITING_APPROVAL待确认，APPROVED已批准，REJECTED已拒绝，EXECUTING执行中，COMPLETED已完成，FAILED失败，CANCELLED已取消
     */
    @TableField("status")
    private String status;

    /**
     * 关联ai_agent_workspace_file.id，保存PLAN.md文件记录
     */
    @TableField("plan_file_id")
    private Long planFileId;

    /**
     * 计划文件相对路径，例如plans/PLAN.md
     */
    @TableField("plan_file_path")
    private String planFilePath;

    /**
     * 计划内容快照，方便后台展示和搜索；完整文件仍以workspace为准
     */
    @TableField("plan_content")
    private String planContent;

    /**
     * 计划风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 风险摘要，例如会修改哪些资源、调用哪些高危工具
     */
    @TableField("risk_summary")
    private String riskSummary;

    /**
     * 预期结果或验收标准
     */
    @TableField("expected_result")
    private String expectedResult;

    /**
     * 审批人用户ID
     */
    @TableField("approved_by")
    private Long approvedBy;

    /**
     * 审批时间
     */
    @TableField("approved_at")
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    @TableField("approval_comment")
    private String approvalComment;

    /**
     * 计划开始执行时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 计划完成时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
