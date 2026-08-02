package com.zw.agent.factory.RAGFactory.vector;

import com.zw.agent.entity.AiKnowledgeBaseEntity;

public interface VectorStoreProvider {

    VectorStoreType type();

    boolean isConfigured();

    VectorStoreSession create(AiKnowledgeBaseEntity knowledgeBase);

    void deleteCollection(AiKnowledgeBaseEntity knowledgeBase);
}
