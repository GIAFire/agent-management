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
 * Agent 定义表：保存一个可视化 Agent 的基础身份信息
 * </p>
 *
 * @author 智纬
 * @since 2026-07-12
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_sys_prompt")
public class AiAgentSysPromptEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 提示词主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 提示词模板名称
     */
    @TableField("prompt_name")
    private String promptName;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 提示词内容
     */
    @TableField("sys_prompt")
    private String sysPrompt;

    /**
     * 状态：1启用，0停用，2草稿
     */
    @TableField("status")
    private Byte status;
}
