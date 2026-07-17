package com.zw.agent.runtime.message;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class RuntimeMessageDraft {

    private String runtimeKey;
    private String parentRuntimeKey;
    private String role;
    private String messageType;
    private String messageStatus;
    private String senderName;
    private String contentFormat;
    private StringBuilder textBuffer;
    private Map<String, Object> content;
    private String refType;
    private Long refId;
    private String refKey;
    private String externalMessageId;
    private String finishReason;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;

    public void append(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        if (textBuffer == null) {
            textBuffer = new StringBuilder();
        }
        textBuffer.append(delta);
    }

    public String getTextContent() {
        return textBuffer == null ? null : textBuffer.toString();
    }

    public void complete() {
        messageStatus = "COMPLETED";
        finishedAt = LocalDateTime.now();
        fillDuration();
    }

    public void cancel() {
        messageStatus = "CANCELLED";
        finishedAt = LocalDateTime.now();
        fillDuration();
    }

    public void fail(String code, String message) {
        messageStatus = "FAILED";
        errorCode = code;
        errorMessage = message;
        finishedAt = LocalDateTime.now();
        fillDuration();
    }

    private void fillDuration() {
        if (startedAt != null && finishedAt != null) {
            durationMs = Duration.between(startedAt, finishedAt).toMillis();
        }
    }
}
