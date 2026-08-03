package com.zhiran.agent.factory.RAGFactory.vector;

import com.zhiran.agent.config.vector.VectorStoreProperties;
import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.QdrantStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class QdrantVectorStoreProvider implements VectorStoreProvider {

    private static final Duration MANAGEMENT_TIMEOUT = Duration.ofSeconds(30);

    private final VectorStoreProperties properties;

    public QdrantVectorStoreProvider(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public VectorStoreType type() {
        return VectorStoreType.QDRANT;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getQdrant().getLocation());
    }

    @Override
    public VectorStoreSession create(
            AiKnowledgeBaseEntity knowledgeBase
    ) {
        String collectionName = VectorStoreProviderSupport.collectionName(
                properties,
                knowledgeBase
        );
        VectorStoreProperties.Qdrant config = properties.getQdrant();
        QdrantConnection connection = parseConnection(config.getLocation());
        QdrantStore.Builder builder = QdrantStore.builder()
                .location(config.getLocation())
                .collectionName(collectionName)
                .dimensions(knowledgeBase.getEmbeddingDimension())
                .useTransportLayerSecurity(connection.tls())
                .checkCompatibility(config.isCheckCompatibility());
        if (StringUtils.hasText(config.getApiKey())) {
            builder.apiKey(config.getApiKey());
        }
        try {
            return new VectorStoreSession(builder.build());
        } catch (VectorStoreException error) {
            throw new KnowledgeOperationException(
                    "无法创建 Qdrant Store",
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
        VectorStoreProperties.Qdrant config = properties.getQdrant();
        try (QdrantClient client = createClient(config)) {
            List<String> collections = client.listCollectionsAsync(
                    MANAGEMENT_TIMEOUT
            ).get(
                    MANAGEMENT_TIMEOUT.toSeconds(),
                    TimeUnit.SECONDS
            );
            if (collections.contains(collectionName)) {
                client.deleteCollectionAsync(
                        collectionName,
                        MANAGEMENT_TIMEOUT
                ).get(
                        MANAGEMENT_TIMEOUT.toSeconds(),
                        TimeUnit.SECONDS
                );
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KnowledgeOperationException(
                    "删除 Qdrant collection 时线程被中断",
                    interrupted
            );
        } catch (ExecutionException | TimeoutException error) {
            throw new KnowledgeOperationException(
                    "无法删除 Qdrant collection",
                    error
            );
        }
    }

    private static QdrantClient createClient(
            VectorStoreProperties.Qdrant config
    ) {
        QdrantConnection connection = parseConnection(config.getLocation());
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                connection.host(),
                connection.port(),
                connection.tls(),
                config.isCheckCompatibility()
        );
        if (StringUtils.hasText(config.getApiKey())) {
            builder.withApiKey(config.getApiKey());
        }
        return new QdrantClient(builder.build());
    }

    private static QdrantConnection parseConnection(String location) {
        try {
            URI uri = URI.create(location);
            String scheme = uri.getScheme() == null
                    ? null
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException(
                        "Qdrant location 必须是 http 或 https URL"
                );
            }
            int configuredPort = uri.getPort();
            int grpcPort = configuredPort == -1 || configuredPort == 6333
                    ? 6334
                    : configuredPort;
            return new QdrantConnection(
                    uri.getHost(),
                    grpcPort,
                    "https".equals(scheme)
            );
        } catch (RuntimeException error) {
            throw new KnowledgeOperationException(
                    "Qdrant location 配置无效",
                    error
            );
        }
    }

    private record QdrantConnection(
            String host,
            int port,
            boolean tls
    ) {
    }
}
