package com.zw.agent.factory.RAGFactory.vector;

import com.zw.agent.config.vector.VectorStoreProperties;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.knowledge.KnowledgeOperationException;
import org.springframework.util.StringUtils;

public final class VectorStoreProviderSupport {

    private static final String DEFAULT_PREFIX = "zhiran_rag";

    private VectorStoreProviderSupport() {
    }

    public static String collectionName(
            VectorStoreProperties properties,
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        validateKnowledgeBase(knowledgeBase);
        String prefix = StringUtils.hasText(properties.getCollectionPrefix())
                ? properties.getCollectionPrefix()
                : DEFAULT_PREFIX;
        return prefix + "_" + knowledgeBase.getCollectionName();
    }

    public static void validateKnowledgeBase(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            throw new KnowledgeOperationException("知识库配置不存在");
        }
        if (!StringUtils.hasText(knowledgeBase.getCollectionName())) {
            throw new KnowledgeOperationException(
                    "知识库未配置 collectionName，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }
        if (knowledgeBase.getEmbeddingDimension() == null
                || knowledgeBase.getEmbeddingDimension() <= 0) {
            throw new KnowledgeOperationException(
                    "知识库未配置有效的 embeddingDimension，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }
        if (!"COSINE".equalsIgnoreCase(knowledgeBase.getMetricType())) {
            throw new KnowledgeOperationException(
                    "向量存储只支持 COSINE 距离度量，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }
    }
}
