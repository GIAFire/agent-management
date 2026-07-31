package com.zw.agent.knowledge.dto;

public record KnowledgeMetricsResponse(
        long totalKnowledgeBases,
        long enabledKnowledgeBases,
        long totalDocuments,
        long newDocumentsToday,
        long readyDocuments,
        Double documentReadyRate,
        long totalChunks,
        long totalTokens
) {
}
