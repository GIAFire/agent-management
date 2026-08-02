package com.zw.agent.factory.RAGFactory.vector;

import com.zw.agent.config.vector.VectorStoreProperties;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.knowledge.KnowledgeOperationException;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.MilvusStore;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MilvusVectorStoreProvider implements VectorStoreProvider {

    private final VectorStoreProperties properties;

    public MilvusVectorStoreProvider(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public VectorStoreType type() {
        return VectorStoreType.MILVUS;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getMilvus().getHost());
    }

    @Override
    public VectorStoreSession create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String collectionName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.Milvus config = properties.getMilvus();
        MilvusStore.Builder builder = MilvusStore.builder()
                .uri(config.getHost())
                .databaseName(databaseName(config))
                .collectionName(collectionName)
                .dimensions(knowledgeBase.getEmbeddingDimension())
                .connectTimeoutMs(config.getConnectTimeout().toMillis())
                .metricType(IndexParam.MetricType.COSINE);
        applyAuthentication(builder, config);
        try {
            return new VectorStoreSession(builder.build());
        } catch (VectorStoreException error) {
            throw new KnowledgeOperationException(
                    "无法创建 Milvus Store",
                    error
            );
        }
    }

    @Override
    public void deleteCollection(AiKnowledgeBaseEntity knowledgeBase) {
        String collectionName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.Milvus config = properties.getMilvus();
        MilvusClientV2 client = null;
        try {
            client = new MilvusClientV2(connectConfig(config));
            String databaseName = databaseName(config);
            Boolean exists = client.hasCollection(
                    HasCollectionReq.builder()
                            .databaseName(databaseName)
                            .collectionName(collectionName)
                            .build()
            );
            if (Boolean.TRUE.equals(exists)) {
                client.dropCollection(
                        DropCollectionReq.builder()
                                .databaseName(databaseName)
                                .collectionName(collectionName)
                                .build()
                );
            }
        } catch (RuntimeException error) {
            throw new KnowledgeOperationException(
                    "无法删除 Milvus collection",
                    error
            );
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private static ConnectConfig connectConfig(
            VectorStoreProperties.Milvus config
    ) {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(config.getHost())
                .dbName(databaseName(config))
                .connectTimeoutMs(config.getConnectTimeout().toMillis());
        if (StringUtils.hasText(config.getToken())) {
            builder.token(config.getToken());
        } else {
            if (StringUtils.hasText(config.getUsername())) {
                builder.username(config.getUsername());
            }
            if (StringUtils.hasText(config.getPassword())) {
                builder.password(config.getPassword());
            }
        }
        return builder.build();
    }

    private static void applyAuthentication(
            MilvusStore.Builder builder,
            VectorStoreProperties.Milvus config
    ) {
        if (StringUtils.hasText(config.getToken())) {
            builder.token(config.getToken());
            return;
        }
        if (StringUtils.hasText(config.getUsername())) {
            builder.username(config.getUsername());
        }
        if (StringUtils.hasText(config.getPassword())) {
            builder.password(config.getPassword());
        }
    }

    private static String databaseName(
            VectorStoreProperties.Milvus config
    ) {
        return StringUtils.hasText(config.getDatabase())
                ? config.getDatabase()
                : "default";
    }
}
