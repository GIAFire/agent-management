package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.entity.BaseEntity;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文
 * </p>
 *
 * @author 
 * @since 2026-06-20
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
     * 关联 ai_agent_session.id
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 关联 ai_agent_run.id，用户消息可为空
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 会话内消息序号
     */
    @TableField("seq")
    private Integer seq;

    /**
     * 消息角色：USER/ASSISTANT/SYSTEM/TOOL
     */
    @TableField("role")
    private String role;

    /**
     * 发送方名称
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 纯文本内容，便于搜索和列表展示
     */
    @TableField("text_content")
    private String textContent;

    /**
     * AgentScope Msg 的 ContentBlock JSON 原始内容
     */
    @TableField("content_json")
    private String contentJson;

    /**
     * token 用量JSON，仅 assistant 消息通常有值
     */
    @TableField("usage_json")
    private String usageJson;

    /**
     * 生成结束原因，例如 stop/tool_call/length
     */
    @TableField("generate_reason")
    private String generateReason;

    /**
     * AgentScope Msg 原始ID
     */
    @TableField("external_msg_id")
    private String externalMsgId;
}
