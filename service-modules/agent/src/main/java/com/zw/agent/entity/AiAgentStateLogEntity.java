package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_state_log")
public class AiAgentStateLogEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 状态引用主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置版本ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 平台会话ID，关联ai_agent_session.id
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 平台用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 传给RuntimeContext.userId的值，例如tenantId-userId
     */
    @TableField("runtime_user_key")
    private String runtimeUserKey;

    /**
     * 传给RuntimeContext.sessionId的值，需要全局唯一
     */
    @TableField("runtime_session_key")
    private String runtimeSessionKey;

    /**
     * 状态后端：REDIS/MYSQL/OSS/JSON_FILE/IN_MEMORY
     */
    @TableField("state_backend")
    private String stateBackend;

    /**
     * AgentState在后端存储中的key
     */
    @TableField("state_key")
    private String stateKey;

    /**
     * 序列化后状态大小，单位字节
     */
    @TableField("state_size_bytes")
    private Long stateSizeBytes;

    /**
     * AgentState上下文消息数量
     */
    @TableField("context_message_count")
    private Integer contextMessageCount;

    /**
     * 是否已经产生上下文摘要：1是，0否
     */
    @TableField("summary_exists")
    private Byte summaryExists;

    /**
     * 摘要预览，用于后台排查，不保存完整敏感内容
     */
    @TableField("summary_preview")
    private String summaryPreview;

    /**
     * 最后一次上下文压缩时间
     */
    @TableField("last_compacted_at")
    private LocalDateTime lastCompactedAt;

    /**
     * 最后一次加载状态时间
     */
    @TableField("last_loaded_at")
    private LocalDateTime lastLoadedAt;

    /**
     * 最后一次保存状态时间
     */
    @TableField("last_saved_at")
    private LocalDateTime lastSavedAt;

    /**
     * 状态过期时间，用于清理长时间不用的会话状态
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;
}
