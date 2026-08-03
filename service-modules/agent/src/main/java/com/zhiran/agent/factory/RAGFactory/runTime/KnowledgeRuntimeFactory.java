package com.zhiran.agent.factory.RAGFactory.runTime;

import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.factory.RAGFactory.EmbeddingModelFactory;
import com.zhiran.agent.factory.RAGFactory.VectorStoreFactory;
import com.zhiran.agent.factory.RAGFactory.vector.VectorStoreSession;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.RetrieveConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class KnowledgeRuntimeFactory {

    private final VectorStoreFactory vectorStoreFactory;
    private final EmbeddingModelFactory embeddingModelFactory;

    public KnowledgeRuntime create(AiKnowledgeBaseEntity knowledgeBase) {
        validate(knowledgeBase);
        VectorStoreSession vectorStoreSession =
                vectorStoreFactory.create(knowledgeBase);
        try {
            EmbeddingModel embeddingModel =
                    embeddingModelFactory.create(knowledgeBase);

            SimpleKnowledge simpleKnowledge =
                    SimpleKnowledge.builder()
                            .embeddingModel(embeddingModel)
                            .embeddingStore(vectorStoreSession.store())
                            .build();

            RetrieveConfig retrieveConfig =
                    RetrieveConfig.builder()
                            .limit(Math.min(
                                    defaultValue(
                                            knowledgeBase.getTopK(),
                                            5
                                    ) * 5,
                                    100
                            ))
                            .scoreThreshold(0D)
                            .build();

            return new KnowledgeRuntime(
                    knowledgeBase.getId(),
                    knowledgeBase.getKnowledgeCode(),
                    knowledgeBase.getKnowledgeName(),
                    knowledgeBase.getCollectionName(),
                    knowledgeBase.getMetricType(),
                    defaultValue(
                            knowledgeBase.getScoreThreshold(),
                            0.5D
                    ),
                    defaultValue(knowledgeBase.getTopK(), 5),
                    simpleKnowledge,
                    retrieveConfig,
                    vectorStoreSession
            );
        } catch (RuntimeException error) {
            vectorStoreSession.close();
            throw error;
        }
    }

    private static int defaultValue(
            Integer value,
            int defaultValue
    ) {
        return value == null ? defaultValue : value;
    }

    private static double defaultValue(
            java.math.BigDecimal value,
            double defaultValue
    ) {
        return value == null ? defaultValue : value.doubleValue();
    }

    private static void validate(AiKnowledgeBaseEntity knowledgeBase) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            throw new IllegalArgumentException("知识库配置不存在");
        }
        if (!StringUtils.hasText(knowledgeBase.getCollectionName())) {
            throw new IllegalStateException("知识库未配置 collectionName，knowledgeBaseId=" + knowledgeBase.getId());
        }
        if (knowledgeBase.getEmbeddingDimension() == null
                || knowledgeBase.getEmbeddingDimension() <= 0) {
            throw new IllegalStateException("知识库未配置有效的 embeddingDimension，knowledgeBaseId=" + knowledgeBase.getId());
        }
    }
}
