package com.zw.agent.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class KnowledgeBaseDTO {

    private Long id;
    private String knowledgeName;
    private String collectionName;
    private String description;
    private String chunkStrategy;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Boolean rerankEnabled;
    private String visibility;
    private Integer status;
    private Map<String, Object> providerMetaJson;
    private Long createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
