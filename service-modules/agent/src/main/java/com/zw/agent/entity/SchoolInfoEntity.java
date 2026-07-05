package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学校信息表
 * </p>
 *
 * @author 智纬智能体平台
 * @since 2026-06-26
 */
@Data
@TableName("school_info")
public class SchoolInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("ding_id")
    private Long dingId;

    @TableField("school_type")
    private Long schoolType;

    @TableField("school_belong")
    private Long schoolBelong;

    @TableField("school_name")
    private String schoolName;

    @TableField("school_address")
    private String schoolAddress;

    @TableField("gaode_location")
    private String gaodeLocation;

    @TableField("admin_id")
    private Long adminId;

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

    @TableField("del_unique_key")
    private Long delUniqueKey;

    @TableField("dt")
    private String dt;

    @TableField("school_type_name")
    private String schoolTypeName;

    @TableField("school_blong_name")
    private String schoolBlongName;

    @TableField("school_belong_name")
    private String schoolBelongName;
}
