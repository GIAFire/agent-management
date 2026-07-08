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
 * Agent 版本表：保存每次可视化配置发布后的不可变快照
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_config")
public class AiAgentConfigEntity extends BaseEntity {

    /**
     * Agent 版本主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 ai_agent_definition.id
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 版本号，例如 v1、v2、2026.06.20-1
     */
    @TableField("version_no")
    private String versionNo;

    /**
     * 系统提示词，对应 AgentScope builder 的 sysPrompt
     */
    @TableField("sys_prompt")
    private String sysPrompt;

    /**
     * 关联 ai_agent_model.id
     */
    @TableField("model_id")
    private Long modelId;

    /**
     * ReAct 最大循环次数，防止无限工具调用
     */
    @TableField("max_iters")
    private Integer maxIters;

    /**
     * 上下文压缩配置JSON，例如 triggerMessages、keepMessages
     */
    @TableField("compaction_config_json")
    private String compactionConfigJson;

    /**
     * 工作区配置JSON，例如 workspace path、隔离级别
     */
    @TableField("workspace_config_json")
    private String workspaceConfigJson;

    /**
     * 全局默认权限模式：ALLOW(全部放行)/ASK(询问用户)/DENY(全部拒绝)
     */
    @TableField("permission_mode")
    private String permissionMode;

    /**
     * Vue3 可视化画布JSON，保存节点、边、位置、表单配置
     */
    @TableField("visual_schema_json")
    private String visualSchemaJson;

    /**
     * 发布状态：0草稿，1已发布，2已废弃
     */
    @TableField("publish_status")
    private Integer publishStatus;

    /**
     * 是否启用记忆：1启用,0禁用
     */
    @TableField("memory_enable")
    private Integer memoryEnable;

    /**
     * 是否启用Plan Mode：1启用，0关闭
     */
    @TableField("plan_mode_enabled")
    private Integer planModeEnabled;

    /**
     * 计划文件目录，相对workspace_path，例如plans
     */
    @TableField("plan_file_directory")
    private Integer planFileDirectory;

    /**
     * 是否启用todo_write任务列表：1启用，0关闭
     */
    @TableField("task_list_enabled")
    private Integer taskListEnabled;

    /**
     * Plan阶段是否允许shell工具：1允许，0禁止；生产环境建议关闭
     */
    @TableField("allow_shell_in_plan_mode")
    private Integer allowShellInPlanMode;

    /**
     * Plan制定完毕后是否需要人工确认：1需要，0不需要
     */
    @TableField("plan_exit_approval_required")
    private Integer planExitApprovalRequired;

    /**
     * 单个计划最多允许的步骤数量
     */
    @TableField("plan_max_steps")
    private Integer planMaxSteps;

    /**
     * 是否允许模型自主进入Plan Mode：1允许，0关闭
     */
    @TableField("plan_auto_enter_enabled")
    private Integer planAutoEnterEnabled;

    /**
     * Plan Mode额外提示词，用于约束计划格式、风险说明和验收标准
     */
    @TableField("plan_prompt")
    private Integer planPrompt;

    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
}
