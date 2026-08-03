package com.zhiran.agent.entity.DTO;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RunOverviewRecentInteractionRow {

    private Long runId;
    private Long sessionId;
    private Long agentId;
    private String agentCode;
    private String agentName;
    private String userMessage;
    private String assistantMessage;
    private String status;
    private LocalDateTime startedAt;
}
