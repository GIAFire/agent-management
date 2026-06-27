package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 工具组配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_tool_group_config")
public class AiToolGroupConfigEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工具组名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 工具组描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 是否默认激活
     */
    @TableField("active_by_default")
    private Boolean activeByDefault;

}
