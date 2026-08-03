package com.zhiran.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record SubagentListItemResponse(
        Long id,
        String subagentCode,
        String subagentName,
        String description,
        Byte sourceType,
        Long localAgentId,
        String remoteUrl,
        Byte protocolType,
        Byte enabled,
        String remark,
        String sourceName,
        boolean sourceAvailable,
        long todayDelegations,
        long parentAgents,
        Double successRate,
        List<HeaderSummary> headers,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt
) {
    public record HeaderSummary(Long id, String headerName, boolean hasValue) {
    }
}
