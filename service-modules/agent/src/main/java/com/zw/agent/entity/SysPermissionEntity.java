package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限表（菜单/接口）
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("sys_permission")
public class SysPermissionEntity extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 权限编码（唯一，如 user:add、user:list）
     */
    @TableField("perm_code")
    private String permCode;

    /**
     * 权限名称（如 用户新增、用户列表）
     */
    @TableField("perm_name")
    private String permName;

    /**
     * 父权限ID（用于菜单/按钮层级，0表示顶级）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 类型：1-菜单，2-按钮/接口
     */
    @TableField("type")
    private Integer type;

    /**
     * 排序号
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;
}
