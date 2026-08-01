package com.zw.agent.knowledge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeBaseResponse {

    private Long id;

    private String knowledgeName;

    private String description;

    private String modelUrl;

    private String embeddingModelName;

    private Integer embeddingDimension;

    private String metricType;

    private Integer topK;

    private BigDecimal scoreThreshold;

    private Byte status;

    private Long documentCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
