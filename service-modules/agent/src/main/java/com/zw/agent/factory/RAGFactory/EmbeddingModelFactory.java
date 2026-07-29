package com.zw.agent.factory.RAGFactory;

import com.zw.agent.entity.AiKnowledgeBaseEntity;
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
        String environmentKey =
                knowledgeBase.getApiKeyRef();

        if (!StringUtils.hasText(environmentKey)) {
            throw new IllegalStateException(
                    "知识库未配置 Embedding API Key 环境变量名称，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }

        String apiKey = System.getenv(environmentKey);

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "Embedding API Key 不存在，environmentKey="
                            + environmentKey
            );
        }

        return apiKey;
    }
}
