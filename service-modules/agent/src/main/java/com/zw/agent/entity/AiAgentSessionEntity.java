package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_session")
public class AiAgentSessionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会话主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 ai_agent_definition.id
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 本会话使用的 Agent 配置ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 平台用户ID，可为空表示匿名用户
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 传给 AgentScope RuntimeContext 的 userId，需要使用跨平台文件名安全格式
     */
    @TableField("runtime_user_key")
    private String runtimeUserKey;

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * AgentState 外部存储 key，后续接 Redis/MySQL 状态存储
     */
    @TableField("state_store_key")
    private String stateStoreKey;

    /**
     * 状态：1正常，0关闭
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后消息时间
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;
}
