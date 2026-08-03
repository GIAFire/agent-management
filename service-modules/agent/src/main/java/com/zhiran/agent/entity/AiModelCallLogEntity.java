package com.zhiran.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiran.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_model_call_log")
public class AiModelCallLogEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("model_config_id")
    private Long modelConfigId;

    @TableField("run_id")
    private Long runId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("agent_config_id")
    private Long agentConfigId;

    @TableField("call_source")
    private String callSource;

    @TableField("source_path")
    private String sourcePath;

    @TableField("status")
    private String status;

    @TableField("config_name_snapshot")
    private String configNameSnapshot;

    @TableField("provider_name_snapshot")
    private String providerNameSnapshot;

    @TableField("protocol_snapshot")
    private String protocolSnapshot;

    @TableField("model_name_snapshot")
    private String modelNameSnapshot;

    @TableField("input_tokens")
    private Integer inputTokens;

    @TableField("output_tokens")
    private Integer outputTokens;

    @TableField("cached_tokens")
    private Integer cachedTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;
}
