package com.zw.agent.entity.message;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentInterventionRequest {
    private Long agentId;
    private Long sessionId;
    private Long runId;
    private String replyId;
    private List<ToolConfirmRequest> confirmResults;
    private List<ToolExecutionResultRequest> toolResults;

    @Data
    public static class ToolConfirmRequest {
        private Boolean confirmed;
        private ToolCallRequest toolCall;
    }

    @Data
    public static class ToolCallRequest {
        private String id;
        private String name;
        private Map<String, Object> input;
        private String content;
        private Map<String, Object> metadata;
    }

    @Data
    public static class ToolExecutionResultRequest {
        private String toolCallId;
        private String toolName;
        private String text;
        private Boolean success;
        private String state;
        private Map<String, Object> metadata;
    }
}
