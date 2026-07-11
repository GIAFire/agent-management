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
 * 主Agent与子Agent绑定表：定义某个主Agent版本可以委派哪些子Agent及调用策略
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_subagent_agent_binding")
public class AiSubagentAgentBindingEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主子Agent绑定ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 主Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 主Agent配置版本ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 子Agent定义ID
     */
    @TableField("subagent_id")
    private Long subagentId;

    /**
     * 绑定别名；为空则使用subagent_key
     */
    @TableField("alias")
    private String alias;

    /**
     * 是否启用：1启用，0停用
     */
    @TableField("enabled")
    private Byte enabled;

    /**
     * 是否暴露给主Agent选择：1是，0否
     */
    @TableField("visible_to_parent")
    private Byte visibleToParent;

    /**
     * 是否允许暴露给用户直接对话：1是，0否，NULL沿用子Agent定义
     */
    @TableField("expose_to_user")
    private Byte exposeToUser;

    /**
     * 默认同步等待时间；0表示默认后台任务
     */
    @TableField("default_timeout_seconds")
    private Integer defaultTimeoutSeconds;

    /**
     * 允许的最大等待时间
     */
    @TableField("max_timeout_seconds")
    private Integer maxTimeoutSeconds;

    /**
     * 该子Agent最大并行后台任务数
     */
    @TableField("max_parallel_tasks")
    private Integer maxParallelTasks;

    /**
     * 是否继承父Agent权限限制：1继承，0不继承；生产建议继承
     */
    @TableField("inherit_parent_permissions")
    private Byte inheritParentPermissions;

    /**
     * 是否继承父Agent长期记忆：1继承，0不继承
     */
    @TableField("inherit_parent_memory")
    private Byte inheritParentMemory;

    /**
     * 是否继承父Agent知识库：1继承，0不继承
     */
    @TableField("inherit_parent_knowledge")
    private Byte inheritParentKnowledge;
}
