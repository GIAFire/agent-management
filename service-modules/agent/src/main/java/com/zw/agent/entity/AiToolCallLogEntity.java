package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName(value = "ai_tool_call_log", autoResultMap = true)
public class AiToolCallLogEntity extends BaseEntity {

    /**
     * 工具调用审计主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Agent运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 回复消息ID
     */
    @TableField("reply_id")
    private String replyId;

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
     * 触发用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 工具ID
     */
    @TableField("tool_id")
    private Long toolId;

    /**
     * 工具名称
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * AgentScope工具调用ID
     */
    @TableField("tool_call_id")
    private String toolCallId;

    /**
     * 最终权限结果：ALLOW/DENY/ASK/PASSTHROUGH
     */
    @TableField("permission_behavior")
    private String permissionBehavior;

    /**
     * 命中的平台权限规则ID
     */
    @TableField("permission_rule_id")
    private Long permissionRuleId;

    /**
     * 工具入参JSON，敏感字段需要脱敏
     */
    @TableField(value = "tool_input_json",typeHandler = JacksonTypeHandler.class)
    private Map<String,Object> toolInputJson;

    /**
     * 工具输出JSON，敏感字段需要脱敏或截断
     */
    @TableField(value = "tool_output_json",typeHandler = JacksonTypeHandler.class)
    private Map<String,Object> toolOutputJson;

    /**
     * 是否执行成功：1成功，0失败，ASK未执行时可为空
     */
    @TableField("success_status")
    private String successStatus;

    /**
     * 工具开始执行时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 工具结束执行时间
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;

    /**
     * 工具执行耗时，单位毫秒
     */
    @TableField("duration_ms")
    private Long durationMs;

    @TableField(exist = false)
    private StringBuilder toolInputBuffer;

    @TableField(exist = false)
    private StringBuilder toolOutputBuffer;

    @Override
    public String toString() {
        return "AiToolCallAuditEntity{" +
                "id=" + id +
                ", runId=" + runId +
                ", sessionId=" + sessionId +
                ", replyId='" + replyId + '\'' +
                ", agentId=" + agentId +
                ", agentConfigId=" + agentConfigId +
                ", userId=" + userId +
                ", toolId=" + toolId +
                ", toolName='" + toolName + '\'' +
                ", toolCallId='" + toolCallId + '\'' +
                ", permissionBehavior='" + permissionBehavior + '\'' +
                ", permissionRuleId=" + permissionRuleId +
                ", toolInputJson='" + toolInputJson + '\'' +
                ", toolOutputJson='" + toolOutputJson + '\'' +
                ", successStatus='" + successStatus + '\'' +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", durationMs=" + durationMs +
                '}';
    }
}
