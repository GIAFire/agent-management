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
 * Skill角色权限表：可配置skill角色权限
 * </p>
 *
 * @author 智伟
 * @since 2026-07-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_skill_role")
public class AiSkillRoleEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Skill定义ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * skill_info_id
     */
    @TableField("skill_info_id")
    private Long skillInfoId;

    /**
     * 角色code
     */
    @TableField("role_code")
    private String roleCode;
}
