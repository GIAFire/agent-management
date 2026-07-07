package com.zw.agent.config;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.InMemoryStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RAGConfig {
    @Bean
    EmbeddingModel ragEmbeddingModel() {
        return DashScopeTextEmbedding.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("text-embedding-v3")
                .dimensions(1024)
                .build();
    }

    @Bean
    VDBStoreBase ragVectorStore() {
        return InMemoryStore.builder()
                .dimensions(1024)
                .build();
    }

    @Bean
    Knowledge ragKnowledge(EmbeddingModel ragEmbeddingModel, VDBStoreBase ragVectorStore) {
        SimpleKnowledge simpleKnowledge = SimpleKnowledge.builder()
                .embeddingModel(ragEmbeddingModel)
                .embeddingStore(ragVectorStore)
                .build();
        return SimpleKnowledge.builder()
                .embeddingModel(ragEmbeddingModel)
                .embeddingStore(ragVectorStore)
                .build();
    }

}
