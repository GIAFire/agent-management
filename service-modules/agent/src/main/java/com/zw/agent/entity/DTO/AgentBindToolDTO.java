package com.zw.agent.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 全局工具配置表
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Data
@Accessors(chain = true)
public class AgentBindToolDTO{

    private Long id;

    private Long agentId;
    private String toolName;
    private String groupId;
    private String permissionCode;
    private String toolNameExplain;
    private String description;
    private String toolKey;
    private String signatureHash;
    private String toolType;
    private String beanName;
    private String className;
    private String methodName;
    private String inputSchema;
    private String outputSchema;
    private Boolean readOnly;
    private Boolean concurrency;
    private Boolean stateInjected;
    private String riskLevel;
    private Boolean enabled;
    private Integer timeoutMs;
    private Integer maxRetries;
    private String matchRule;
    private String defaultGroupCode;

}
