package com.zw.agent.factory.ragFactory;

import com.zw.agent.entity.AiKnowledgeBackendConfigEntity;
import com.zw.agent.factory.ragFactory.backendImpl.KnowledgeBackend;
import com.zw.agent.factory.ragFactory.backendImpl.SelfManagedKnowledgeBackend;
import com.zw.agent.factory.ragFactory.entity.KnowledgeBase;
import com.zw.common.utils.AESUtil;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RagBackendFactory {

    private static final String DEFAULT_COLLECTION_NAME = "default_collection";

    public KnowledgeBackend create(AiKnowledgeBackendConfigEntity config) {
        return create(config, new KnowledgeBase());
    }

    public KnowledgeBackend create(AiKnowledgeBackendConfigEntity config, KnowledgeBase knowledgeBase) {
        if (config == null) {
            throw new IllegalArgumentException("Knowledge backend config must not be null");
        }

        String backendStoreType = normalize(config.getBackendStoreType());
        if ("SELF_MANAGED".equals(backendStoreType)) {
            return createSelfManagedBackend(config, knowledgeBase);
        }

        if ("RAGFLOW".equals(backendStoreType) || "DIFY".equals(backendStoreType)) {
            throw new UnsupportedOperationException("External RAG backend is not implemented yet: " + config.getBackendStoreType());
        }

        throw new IllegalArgumentException("Unsupported backend type: " + config.getBackendStoreType());
    }

    private KnowledgeBackend createSelfManagedBackend(AiKnowledgeBackendConfigEntity config, KnowledgeBase knowledgeBase) {
        EmbeddingModel embeddingModel = createEmbeddingModel(config);
        EmbeddingStore<TextSegment> embeddingStore = createEmbeddingStore(config, knowledgeBase);
        return new SelfManagedKnowledgeBackend(embeddingModel, embeddingStore);
    }

    private EmbeddingModel createEmbeddingModel(AiKnowledgeBackendConfigEntity config) {
        if (config.getApiType() == null) {
            throw new IllegalArgumentException("Embedding api type must not be null");
        }

        String apiKey = decryptSecret(config.getApiKeyRef());
        String apiType = normalize(config.getApiType().getCode());

        return switch (apiType) {
            case "OPENAI" -> OpenAiEmbeddingModel.builder()
                    .baseUrl(config.getModelUrl())
                    .apiKey(apiKey)
                    .modelName(config.getEmbeddingModelName())
//                    .dimensions(config.getEmbeddingDimension())
                    .build();

            case "DASHSCOPE", "DASH_SCOPE" -> QwenEmbeddingModel.builder()
                    .baseUrl(config.getModelUrl())
                    .apiKey(apiKey)
                    .modelName(config.getEmbeddingModelName())
                    .dimension(config.getEmbeddingDimension())
                    .build();

            case "ZHIPU" -> ZhipuAiEmbeddingModel.builder()
                    .baseUrl(config.getModelUrl())
                    .apiKey(apiKey)
                    .model(config.getEmbeddingModelName())
                    .dimensions(config.getEmbeddingDimension())
                    .build();

            case "OLLAMA" -> OllamaEmbeddingModel.builder()
                    .baseUrl(config.getEndpoint())
                    .modelName(config.getEmbeddingModelName())
                    .build();

            default -> throw new IllegalArgumentException("Unsupported embedding api type: " + config.getApiType().getCode());
        };
    }

    private EmbeddingStore<TextSegment> createEmbeddingStore(AiKnowledgeBackendConfigEntity config, KnowledgeBase knowledgeBase) {
        String databaseType = normalize(config.getDatabaseType());
        String collectionName = collectionName(knowledgeBase);
        String apiKey = decryptSecret(config.getApiKeyRef());

        return switch (databaseType) {
            case "QDRANT" -> QdrantEmbeddingStore.builder()
                    .host(config.getEndpoint())
                    .port(requiredPort(config))
                    .collectionName(collectionName)
                    .apiKey(apiKey)
                    .build();

            case "MILVUS" -> MilvusEmbeddingStore.builder()
                    .host(config.getEndpoint())
                    .port(requiredPort(config))
                    .collectionName(collectionName)
                    .dimension(config.getEmbeddingDimension())
                    .indexType(IndexType.FLAT)
                    .metricType(MetricType.COSINE)
                    .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                    .autoFlushOnInsert(true)
                    .idFieldName("id")
                    .textFieldName("text")
                    .metadataFieldName("metadata")
                    .vectorFieldName("vector")
                    .build();

            default -> throw new IllegalArgumentException("Unsupported vector store: " + config.getDatabaseType());
        };
    }

    private String collectionName(KnowledgeBase knowledgeBase) {
        if (knowledgeBase != null && hasText(knowledgeBase.getCollectionName())) {
            return knowledgeBase.getCollectionName();
        }
        return DEFAULT_COLLECTION_NAME;
    }

    private Integer requiredPort(AiKnowledgeBackendConfigEntity config) {
        if (config.getEndpointPort() == null) {
            throw new IllegalArgumentException("Endpoint port must not be null");
        }
        return config.getEndpointPort();
    }

    private String decryptSecret(String secret) {
        if (!hasText(secret)) {
            return null;
        }
        try {
            String decrypted = AESUtil.decrypt(secret);
            return hasText(decrypted) ? decrypted : secret;
        } catch (Exception ignored) {
            return secret;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
