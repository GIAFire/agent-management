package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_run_event_log")
public class AiAgentRunEventLogEntity extends BaseEntity {

    /**
     * 事件主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联 ai_agent_run.id
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 关联 session.id
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * AgentScope 事件类型，例如 TEXT_BLOCK_DELTA、TOOL_CALL_START
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件来源路径，后续用于主 Agent/子 Agent 区分
     */
    @TableField("source_path")
    private String sourcePath;

    /**
     * 事件完整 JSON 载荷
     */
    @TableField("payload_json")
    private String payloadJson;

}
