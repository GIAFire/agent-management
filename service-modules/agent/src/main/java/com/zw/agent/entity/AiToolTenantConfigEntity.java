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
 * 租户工具授权配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_tool_tenant_config")
public class AiToolTenantConfigEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工具编码，对应 tool_config.tool_code
     */
    @TableField("tool_code")
    private String toolCode;

    /**
     * 租户侧工具组编码，可覆盖 tool_config.default_group_code
     */
    @TableField("group_code")
    private String groupCode;

    /**
     * 业务权限编码，例如 order:query
     */
    @TableField("permission_code")
    private String permissionCode;
}
