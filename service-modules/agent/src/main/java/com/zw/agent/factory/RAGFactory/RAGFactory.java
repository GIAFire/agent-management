package com.zw.agent.factory.RAGFactory;


import com.github.benmanes.caffeine.cache.Cache;
import com.zw.agent.entity.DTO.AgentConfigDTO;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RAGFactory {

    private final Cache<String, EmbeddingModel> embeddingModelCache;

    public EmbeddingModel buildEmbeddingModel(
            AgentConfigDTO config
    ){
        EmbeddingModel embeddings = null;
        if (EmbeddinModelType.SIMPLE.equals(config.getStoreType())){
            embeddings = DashScopeTextEmbedding.builder()
                    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .modelName("text-embedding-v3")
                    .dimensions(1024)
                    .build();
        }

        return embeddings;
    }
}
