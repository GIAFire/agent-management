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

    private static final long serialVersionUID = 1L;

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
     * 关联 ai_model_config.id
     */
    @TableField("model_config_id")
    private Long modelConfigId;

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
     * 默认权限模式：ALLOW/ASK/DENY/EXPLORE 等平台自定义映射
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
    private Byte publishStatus;

    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
}
