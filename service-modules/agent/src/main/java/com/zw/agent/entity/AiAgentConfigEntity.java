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
     * 系统提示词，关联 ai_agent_sys_prompt.id
     */
    @TableField("sys_prompt_id")
    private Long sysPromptId;

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
     * 工作区目录，例如/local
     */
    @TableField("workspace_path")
    private String workspacePath;

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
     * agent全局权限策略id
     */
    @TableField("agent_permission_policy_id")
    private Long agentPermissionPolicyId;

    /**
     * 发布状态：0草稿，1已发布，2已废弃
     */
    @TableField("publish_status")
    private Integer publishStatus;

    @TableField("context_enabled")
    private Integer contextEnabled;

    @TableField("trigger_messages")
    private Integer triggerMessages;

    @TableField("keep_messages")
    private Integer keepMessages;

    @TableField("trigger_tokens")
    private Integer triggerTokens;

    @TableField("keep_tokens")
    private Integer keepTokens;

    @TableField("flush_before_compact")
    private Integer flushBeforeCompact;

    @TableField("offload_before_compact")
    private Integer offloadBeforeCompact;

    @TableField("compaction_model_config_id")
    private Long compactionModelConfigId;

    @TableField("truncate_args_enabled")
    private Integer truncateArgsEnabled;

    @TableField("truncate_args_max_chars")
    private Integer truncateArgsMaxChars;

    @TableField("tool_result_eviction_enabled")
    private Integer toolResultEvictionEnabled;

    @TableField("tool_result_max_chars")
    private Integer toolResultMaxChars;

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
    private String planFileDirectory;

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
    private String planPrompt;


    /**
     * 是否启用沙箱：1启用，0关闭
     */
    @TableField("sandbox_enabled")
    private Integer sandboxEnabled;

    /**
     * 沙箱配置ID，关联ai_agent_sandbox_config.id
     */
    @TableField("sandbox_config_id")
    private Long sandboxConfigId;

    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
}
