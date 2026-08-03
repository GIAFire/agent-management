package com.zhiran.agent.factory.RAGFactory;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import io.agentscope.core.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmbeddingModelFactory {

    public EmbeddingModel create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String apiKey = resolveApiKey(knowledgeBase);

        return ModelArtsTextEmbedding.builder()
                .baseUrl(knowledgeBase.getModelUrl())
                .apiKey(apiKey)
                .modelName(
                        knowledgeBase.getEmbeddingModelName()
                )
                .dimensions(
                        knowledgeBase.getEmbeddingDimension()
                )
                .build();
    }

    private String resolveApiKey(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String apiKey = knowledgeBase.getApiKey();

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "知识库未配置 Embedding API Key，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }
        return apiKey;
    }
}
