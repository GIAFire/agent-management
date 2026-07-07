package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 租户表：平台多租户隔离的根表
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("sys_tenant")
public class SysTenantEntity implements Serializable {

    /**
     * 租户主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户唯一编码，用于接口、Nacos namespace、日志隔离
     */
    @TableField("tenant_code")
    private String tenantCode;

    /**
     * 租户名称
     */
    @TableField("tenant_name")
    private String tenantName;

    /**
     * 租户状态：1启用，0停用
     */
    @TableField("status")
    private Integer status;

    /**
     * 租户对应的 Nacos 命名空间ID，用于 Agent/Skill 隔离
     */
    @TableField("nacos_namespace_id")
    private String nacosNamespaceId;

    /**
     * 租户备注
     */
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Long updateBy;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Integer deleted;

    @Version
    private Integer version;

}
