package com.zhiran.agent.factory.RAGFactory.vector;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;

public interface VectorStoreProvider {

    VectorStoreType type();

    boolean isConfigured();

    VectorStoreSession create(AiKnowledgeBaseEntity knowledgeBase);

    void deleteCollection(AiKnowledgeBaseEntity knowledgeBase);
}
