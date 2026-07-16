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
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_skill_info")
public class AiSkillInfoEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Skill定义ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Skill唯一编码，对应目录名，例如code-reviewer
     */
    @TableField("skill_key")
    private String skillKey;

    /**
     * Skill显示名称
     */
    @TableField("skill_name")
    private String skillName;

    /**
     * Skill描述，用于Agent判断何时使用该Skill
     */
    @TableField("description")
    private String description;

    /**
     * 完整SKILL.md内容，包括frontmatter和正文
     */
    @TableField("skill_md_content")
    private String skillMdContent;

    /**
     * 风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 是否需要执行脚本或shell：1需要，0不需要
     */
    @TableField("requires_shell")
    private Byte requiresShell;

    /**
     * 是否必须在沙箱中使用：1是，0否
     */
    @TableField("requires_sandbox")
    private Byte requiresSandbox;

    /**
     * 作用域：GLOBAL全局，TENANT租户级，USER用户级，AGENT智能体级
     */
    @TableField("scope_type")
    private String scopeType;

    /**
     * 作用域值，例如userId、agentId；TENANT级可为空
     */
    @TableField("scope_value")
    private String scopeValue;

    /**
     * Skill分类，例如code、data、report、rag、ops
     */
    @TableField("category")
    private String category;

    /**
     * 标签JSON数组，例如["代码审查","Java","Spring"]
     */
    @TableField("tags_json")
    private String tagsJson;

    /**
     * 状态：1启用，0停用
     */
    @TableField("status")
    private Byte status;
}
