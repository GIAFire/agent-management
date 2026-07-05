package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学校设置表
 * </p>
 *
 * @author 智纬智能体平台
 * @since 2026-06-26
 */
@Data
@TableName("base_school_setting")
public class SchoolSettingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("ding_id")
    private Long dingId;

    @TableField("type")
    private Integer type;

    @TableField("name")
    private String name;

    @TableField("del_unique_key")
    private Long delUniqueKey;

    @TableField("delete_status")
    @TableLogic
    private Integer deleteStatus;

    @TableField("create_id")
    private Long createId;

    @TableField("create_by")
    private String createBy;

    @TableField("create_at")
    private LocalDateTime createAt;

    @TableField("update_id")
    private Long updateId;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_at")
    private LocalDateTime updateAt;
}
