package com.zhiran.agent.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeIndexRequest {

    private String chunkStrategy;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String chunkDelimiter;
}
