package com.zhiran.agent.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    private String knowledgeName;

    private String description;

    private String modelUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    private Integer topK;

    private BigDecimal scoreThreshold;

    private Byte status;
}
