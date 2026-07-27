package com.zw.agent.factory.RAGFactory;

import com.zw.agent.config.milvus.MilvusProperties;
import com.zw.agent.entity.DTO.KnowledgeWithBackendDTO;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.MilvusStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.milvus.v2.common.IndexParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MilvusStoreFactory {

    private final MilvusProperties properties;

    public VDBStoreBase create(KnowledgeWithBackendDTO config) throws VectorStoreException {
        MilvusStore.Builder builder = MilvusStore.builder()
                .uri(properties.getUri())
                .databaseName(properties.getDatabaseName())
                .collectionName(config.getCollectionName())
                .dimensions(config.getDimensions())
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

        return builder.build();
    }

    private IndexParam.MetricType resolveMetricType(
            String metricType
    ) {
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