package com.zhiran.agent.knowledge.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeDocumentResponse {

    private Long id;

    private Long knowledgeBaseId;

    private String documentName;

    private String documentType;

    private String mimeType;

    private Long sizeBytes;

    private String checksum;

    private String parseStatus;

    private String chunkStrategy;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String chunkDelimiter;

    private Integer chunkCount;

    private Integer tokenCount;

    private String errorMessage;

    private Byte status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
