package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent消息时间线表：保存用户输入、思考、正文及工具、Skill、子Agent、Plan等可展示消息快照
 * </p>
 *
 * @author 智纬
 * @since 2026-07-18
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_message")
public class AiAgentMessageEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 消息主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID，关联ai_agent_session.id
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 运行ID，关联ai_agent_run_log.id；用户消息创建后可回填
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 会话内全局消息序号，用于历史恢复和稳定排序
     */
    @TableField("seq")
    private Long seq;

    /**
     * 父消息ID，例如工具结果关联工具调用消息
     */
    @TableField("parent_message_id")
    private Long parentMessageId;

    /**
     * 协议角色：USER、ASSISTANT、SYSTEM、TOOL
     */
    @TableField("role")
    private String role;

    /**
     * 消息类型：USER_TEXT、ASSISTANT_THINKING、ASSISTANT_TEXT、TOOL_CALL、TOOL_RESULT、SKILL_CALL、SKILL_RESULT、SUBAGENT_CALL、SUBAGENT_RESULT、PLAN_SNAPSHOT、PLAN_OPERATION、SYSTEM_NOTICE、ERROR
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息状态：STREAMING、COMPLETED、FAILED、CANCELLED
     */
    @TableField("message_status")
    private String messageStatus;

    /**
     * 发送者显示名称，例如用户、Agent、工具、Skill、子Agent名称
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 内容格式：TEXT、MARKDOWN、JSON、MULTIMODAL、CARD
     */
    @TableField("content_format")
    private String contentFormat;

    /**
     * 可搜索和直接展示的文本内容
     */
    @TableField("text_content")
    private String textContent;

    /**
     * 平台标准化内容块或卡片快照，不保存逐Token增量事件
     */
    @TableField("content_json")
    private String contentJson;

    /**
     * content_json结构版本，用于前端兼容升级
     */
    @TableField("content_schema_version")
    private Integer contentSchemaVersion;

    /**
     * 关联业务类型：TOOL_CALL_LOG、SKILL_LOG、SUBAGENT_TASK、AGENT_PLAN、WORKSPACE_FILE
     */
    @TableField("ref_type")
    private String refType;

    /**
     * 关联业务表主键ID
     */
    @TableField("ref_id")
    private Long refId;

    /**
     * 外部关联键，例如tool_call_id、skill_runtime_id、subagent task_id
     */
    @TableField("ref_key")
    private String refKey;

    /**
     * AgentScope或模型侧原始消息ID
     */
    @TableField("external_message_id")
    private String externalMessageId;

    /**
     * Token用量，例如inputTokens、outputTokens、reasoningTokens、totalTokens
     */
    @TableField("usage_json")
    private String usageJson;

    /**
     * 模型结束原因，例如stop、length、tool_calls
     */
    @TableField("finish_reason")
    private String finishReason;

    /**
     * 消息生成或执行错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误信息，返回前端时需要过滤内部敏感信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 消息块或调用开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 消息块或调用结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 耗时，单位毫秒
     */
    @TableField("duration_ms")
    private Long durationMs;
}
