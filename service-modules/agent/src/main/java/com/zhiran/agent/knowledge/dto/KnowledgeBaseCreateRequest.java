package com.zhiran.agent.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    private String knowledgeName;

    private String description;

    private String modelUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    private String embeddingModelName;

    private Integer embeddingDimension;

    private String metricType;

    private Integer topK;

    private BigDecimal scoreThreshold;
}
