package com.zw.agent.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.agent.entity.AiHttpHeaderEntity;
import com.zw.common.entity.BaseEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 子Agent定义表
 * </p>
 *
 * @author 智纬
 * @since 2026-07-26
 */
@Data
public class SubagentHeaderDTO{

    private static final long serialVersionUID = 1L;

    private Long id;

    private String subagentCode;

    private String subagentName;

    private String description;

    /**
     * 来源类型：1.平台Agent 2.远程Agent Protocol
     */
    private Byte sourceType;

    /**
     * 平台内部子Agent ID，source_type=1时使用，关联ai_agent.id
     */
    private Long localAgentId;

    /**
     * 远程Agent Protocol服务基础URL，source_type=2时使用
     */
    private String remoteUrl;

    /**
     * 远程协议类型：1.Agent Protocol，预留2.A2A
     */
    private Byte protocolType;

    /**
     * 是否启用：0.否 1.是
     */
    private Byte enabled;

    /**
     * 备注
     */
    private String remark;

    List<AiHttpHeaderEntity> headerEntityList;
}
