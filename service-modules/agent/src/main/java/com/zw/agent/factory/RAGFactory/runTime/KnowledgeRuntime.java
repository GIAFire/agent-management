package com.zw.agent.factory.RAGFactory.runTime;

import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.RetrieveConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeRuntime {
    private Long knowledgeBaseId;
    private String knowledgeBaseCode;
    private String knowledgeBaseName;
    private String collectionName;
    private SimpleKnowledge knowledge;
    private RetrieveConfig retrieveConfig;
}
