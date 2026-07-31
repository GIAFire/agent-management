package com.zw.agent.entity.DTO;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ModelSaveRequest {
    private Long id;
    private String configName;
    private String providerName;
    private String protocol;
    private String baseURL;
    private String apiKey;
    private Boolean removeApiKey;
    private String description;
    private String modelName;
    private Integer streaming;
    private Integer thinking;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer maxTokens;
    private Long timeoutMs;
    private Integer thinkingBudget;
    private Integer maxAttempts;
    private Integer status;
    private List<ModelHeaderInput> headers;
}
