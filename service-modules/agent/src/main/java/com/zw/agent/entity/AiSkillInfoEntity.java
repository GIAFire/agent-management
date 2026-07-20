package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.util.List;
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
     * Skill显示名称
     */
    @TableField("name")
    private String name;

    /**
     * Skill描述，用于Agent判断何时使用该Skill
     */
    @TableField("description")
    private String description;

    /**
     * SKILL.md正文
     */
    @TableField("skill_content")
    private String skillContent;

    /**
     * SKILL来源
     */
    @TableField("source")
    private String source;

    /**
     * 完整的 YAML 元数据，以 JSON 形式保存
     */
    @TableField("metadata_json")
    private String metadataJson;

    /**
     * 风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    @TableField("risk_level")
    private String riskLevel;

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

    @TableField(exist = false)
    private String skillName;

    @TableField(exist = false)
    private String skillKey;

    @TableField(exist = false)
    private String skillMdContent;

    @TableField(exist = false)
    private String category;

    @TableField(exist = false)
    private List<String> roleCodes;
}
