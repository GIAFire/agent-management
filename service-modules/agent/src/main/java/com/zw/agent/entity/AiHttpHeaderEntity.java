package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.agent.constant.enumeration.HeaderSourceType;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * HTTP请求头配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_http_header")
public class AiHttpHeaderEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联外部资源表id
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 值来源：model、subAgent
     */
    @TableField("source")
    private HeaderSourceType source;

    /**
     * 请求头名称，例如Authorization、X-API-Key
     */
    @TableField("header_name")
    private String headerName;

    /**
     * 请求头值。当前阶段按产品约定明文存储，但禁止通过管理接口回显。
     */
    @TableField("header_value")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String headerValue;
}
