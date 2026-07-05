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
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_state_op_log")
public class AiAgentStateOpLogEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 状态操作日志主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * Agent运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * RuntimeContext.userId
     */
    @TableField("runtime_user_key")
    private String runtimeUserKey;

    /**
     * RuntimeContext.sessionId
     */
    @TableField("runtime_session_key")
    private String runtimeSessionKey;

    /**
     * 操作类型：LOAD/SAVE/COMPACT/EVICT/DELETE/EXPIRE
     */
    @TableField("op_type")
    private String opType;

    /**
     * 状态后端：REDIS/MYSQL/OSS/JSON_FILE/IN_MEMORY
     */
    @TableField("state_backend")
    private String stateBackend;

    /**
     * 状态存储key
     */
    @TableField("state_key")
    private String stateKey;

    /**
     * 操作前状态大小，单位字节
     */
    @TableField("before_size_bytes")
    private Long beforeSizeBytes;

    /**
     * 操作后状态大小，单位字节
     */
    @TableField("after_size_bytes")
    private Long afterSizeBytes;

    /**
     * 操作前上下文消息数
     */
    @TableField("before_message_count")
    private Integer beforeMessageCount;

    /**
     * 操作后上下文消息数
     */
    @TableField("after_message_count")
    private Integer afterMessageCount;

    /**
     * 是否成功：1成功，0失败
     */
    @TableField("success")
    private Integer success;

    /**
     * 失败信息
     */
    @TableField("error_message")
    private String errorMessage;
}
