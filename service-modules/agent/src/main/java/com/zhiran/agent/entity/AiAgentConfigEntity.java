package com.zhiran.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiran.agent.constant.enumeration.StateStoreType;
import com.zhiran.common.entity.BaseEntity;

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
     * 全局默认权限模式：ALLOW(全部放行)/ASK(询问用户)/DENY(全部拒绝)
     */
    @TableField("permission_mode")
    private String permissionMode;

    @TableField("compaction_enabled")
    private Integer compactionEnabled;

    @TableField("trigger_messages")
    private Integer triggerMessages;

    @TableField("keep_messages")
    private Integer keepMessages;

    @TableField("trigger_tokens")
    private Integer triggerTokens;

    @TableField("keep_tokens")
    private Integer keepTokens;

    @TableField("tool_result_eviction_enabled")
    private Integer toolResultEvictionEnabled;

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
     * stateStore类型
     */
    @TableField("state_store_type")
    private StateStoreType stateStoreType;
}
