package com.zw.agent.factory.RAGFactory.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class KnowledgeBase {

    private Long id;

    private Long backendConfigId;

    private String name;

    private String collectionName;

    private String chunkStrategy;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String providerMetaJson;
}
