package com.zw.agent.factory.RAGFactory;

import com.zw.agent.config.milvus.MilvusProperties;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.MilvusStore;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MilvusStoreFactory {

    private final MilvusProperties properties;

    public MilvusStore create(AiKnowledgeBaseEntity config) {
        if (!StringUtils.hasText(config.getCollectionName())) {
            throw new IllegalArgumentException("知识库未配置 collectionName，knowledgeBaseId=" + config.getId());
        }
        if (config.getEmbeddingDimension() == null || config.getEmbeddingDimension() <= 0) {
            throw new IllegalArgumentException("知识库未配置有效的 embeddingDimension，knowledgeBaseId=" + config.getId());
        }
        MilvusStore.Builder builder = MilvusStore.builder()
                .uri(properties.getHost())
                .databaseName(properties.getDatabase())
                .collectionName(config.getCollectionName())
                .dimensions(config.getEmbeddingDimension())
                .connectTimeoutMs(properties.getConnectTimeout().toMillis())
                .metricType(resolveMetricType(config.getMetricType()));

        if (StringUtils.hasText(properties.getToken())) {
            builder.token(properties.getToken());
        } else {
            if (StringUtils.hasText(properties.getUsername())) {
                builder.username(properties.getUsername());
            }

            if (StringUtils.hasText(properties.getPassword())) {
                builder.password(properties.getPassword());
            }
        }

        try {
            return builder.build();
        } catch (VectorStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 释放知识库独占 collection。MilvusStore 的构造是幂等的：collection 已不存在时
     * 会先创建再删除，使失败删除任务可以安全重提。
     */
    public void dropCollection(AiKnowledgeBaseEntity config) {
        try (MilvusStore store = create(config)) {
            store.getClient().dropCollection(
                    DropCollectionReq.builder()
                            .databaseName(store.getDatabaseName())
                            .collectionName(store.getCollectionName())
                            .build()
            );
        } catch (VectorStoreException error) {
            throw new RuntimeException("无法访问 Milvus collection", error);
        }
    }

    private IndexParam.MetricType resolveMetricType(String metricType) {
        if (!StringUtils.hasText(metricType)) {
            return IndexParam.MetricType.COSINE;
        }

        return switch (metricType.toUpperCase()) {
            case "L2" -> IndexParam.MetricType.L2;
            case "IP" -> IndexParam.MetricType.IP;
            case "COSINE" -> IndexParam.MetricType.COSINE;
            default -> throw new IllegalArgumentException(
                    "Unsupported Milvus metric type: " + metricType
            );
        };
    }
}
