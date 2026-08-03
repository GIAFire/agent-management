package com.zhiran.agent.knowledge.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeChunkResponse {

    private Long id;

    private Long documentId;

    private Integer chunkIndex;

    private String chunkUid;

    private String content;

    private String contentHash;

    private String contentType;

    private Integer pageNo;

    private String sectionTitle;

    private Integer startOffset;

    private Integer endOffset;

    private Integer tokenCount;

    private LocalDateTime createdAt;
}
