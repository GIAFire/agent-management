package com.zw.agent.knowledge.dto;

import java.time.LocalDateTime;

public record KnowledgeFailureResponse(
        Long id,
        Long knowledgeBaseId,
        String knowledgeBaseName,
        Long documentId,
        String documentName,
        String taskType,
        String stage,
        String status,
        String errorMessage,
        boolean retryable,
        LocalDateTime finishedAt
) {
}
