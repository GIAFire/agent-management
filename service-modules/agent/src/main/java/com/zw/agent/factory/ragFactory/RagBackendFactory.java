package com.zw.agent.factory.ragFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.zw.agent.entity.AiKnowledgeBackendConfigEntity;
import com.zw.agent.factory.ragFactory.backendImpl.KnowledgeBackend;
import com.zw.agent.factory.ragFactory.backendImpl.SelfManagedKnowledgeBackend;
import com.zw.common.utils.AESUtil;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.stereotype.Component;

@Component
public class RagBackendFactory {

    public KnowledgeBackend create(AiKnowledgeBackendConfigEntity config) {
        if ("RAGFLOW".equalsIgnoreCase(config.getBackendStoreType())) {
//            createRagFlowBackend(config);
            return null;
        }

        if ("DIFY".equalsIgnoreCase(config.getBackendStoreType())) {
//            createDifyBackend(config);
            return null;
        }

        if ("SELF_MANAGED".equalsIgnoreCase(config.getBackendStoreType())) {
            return createSelfManagedBackend(config);
        }

        throw new IllegalArgumentException(
                "Unsupported backend type: " + config.getBackendStoreType()
        );
    }

    private KnowledgeBackend createSelfManagedBackend(AiKnowledgeBackendConfigEntity config) {
        EmbeddingModel embeddingModel = createEmbeddingModel(config);
        EmbeddingStore<TextSegment> embeddingStore = createEmbeddingStore(config);

        return new SelfManagedKnowledgeBackend(embeddingModel, embeddingStore);
    }

    private EmbeddingModel createEmbeddingModel(AiKnowledgeBackendConfigEntity config){

        return switch (config.getApiType().getCode()) {
            case "OPENAI" -> OpenAiEmbeddingModel.builder()
                    .apiKey(AESUtil.decrypt(config.getApiKeyRef()))
                    .modelName(config.getEmbeddingModelName())
                    .dimensions(config.getEmbeddingDimension())
                    .build();

            case "DASHSCOPE" -> QwenEmbeddingModel.builder()
                    .apiKey(AESUtil.decrypt(config.getApiKeyRef()))
                    .modelName(config.getEmbeddingModelName())
                    .build();

            case "ZHIPU" -> ZhipuAiEmbeddingModel.builder()
                    .apiKey(AESUtil.decrypt(config.getApiKeyRef()))
                    .model(config.getEmbeddingModelName())
                    .dimensions(config.getEmbeddingDimension())
                    .build();

            case "OLLAMA" -> OllamaEmbeddingModel.builder()
                    .baseUrl(config.getEndpoint())
                    .modelName(config.getEmbeddingModelName())
                    .build();

            default -> throw new IllegalArgumentException("不支持的类型: " + config.getApiType().getCode());
        };
    }

    private EmbeddingStore<TextSegment> createEmbeddingStore(AiKnowledgeBackendConfigEntity config) {
        return switch (config.getDatabaseType()) {
            case "QDRANT" -> QdrantEmbeddingStore.builder()
                    .host(config.getEndpoint())
                    .port(config.getEndpointPort())
                    .apiKey(AESUtil.decrypt(config.getApiKeyRef()))
                    .build();

            case "MILVUS" -> MilvusEmbeddingStore.builder()
                    .host(config.getEndpoint())
                    .port(config.getEndpointPort())
                    .collectionName("default_collection")
                    .dimension(config.getEmbeddingDimension())
                    .indexType(IndexType.FLAT)                 // 索引类型
                    .metricType(MetricType.COSINE)             // 度量类型
                    .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY)  // 一致性级别
                    .autoFlushOnInsert(true)                   // 插入后自动刷新
                    .idFieldName("id")                         // ID 字段名称
                    .textFieldName("text")                     // 文本字段名称
                    .metadataFieldName("metadata")             // 元数据字段名称
                    .vectorFieldName("vector")                 // 向量字段名称
                    .build();

//            case "ELASTICSEARCH" -> ElasticsearchEmbeddingStore.builder()
//                    .client(new ElasticsearchClient())
//                    .indexName(config)
//                    .build();

            default -> throw new IllegalArgumentException("Unsupported vector store: " + config.getDatabaseType());
        };
    }

}
