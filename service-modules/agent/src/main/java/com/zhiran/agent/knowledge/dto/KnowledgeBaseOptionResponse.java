package com.zhiran.agent.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeBaseOptionResponse {

    private Long id;

    private String knowledgeName;

    private Byte status;
}
