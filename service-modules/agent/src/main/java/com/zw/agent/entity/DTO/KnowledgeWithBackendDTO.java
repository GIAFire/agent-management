package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class KnowledgeWithBackendDTO {

    // ========== 后端配置信息 ==========
    private Long backendId;
    private String backendStoreType;
    private String apiType;
    private String endpoint;
    private Integer endpointPort;
    private String modelUrl;
    private String embeddingModelName;
    private Integer embeddingDimension;
    private String metricType;
    private Integer topK;
    private BigDecimal scoreThreshold;
    private Map<String, Object> configJson;

    // ========== 知识库信息 ==========
    private List<KnowledgeBaseDTO> knowledgeBaseList;
}