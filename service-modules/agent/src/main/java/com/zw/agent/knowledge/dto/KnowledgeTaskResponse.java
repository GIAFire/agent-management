package com.zw.agent.knowledge.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeTaskResponse {

    private Long id;

    private Long knowledgeBaseId;

    private Long documentId;

    private Long retryOfTaskId;

    private String taskType;

    private String status;

    private String stage;

    private Integer progress;

    private Integer completedUnits;

    private Integer totalUnits;

    private String chunkStrategy;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String chunkDelimiter;

    private Integer chunkCount;

    private Integer tokenCount;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
