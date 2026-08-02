package com.zw.agent.factory.RAGFactory.runTime;

import com.zw.agent.factory.RAGFactory.vector.VectorStoreSession;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.RetrieveConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeRuntime implements AutoCloseable {
    private Long knowledgeBaseId;
    private String knowledgeBaseCode;
    private String knowledgeBaseName;
    private String collectionName;
    private String metricType;
    private double scoreThreshold;
    private int resultLimit;
    private SimpleKnowledge knowledge;
    private RetrieveConfig retrieveConfig;
    private VectorStoreSession vectorStoreSession;

    @Override
    public void close() {
        if (knowledge == null) {
            return;
        }
        closeIfNeeded(knowledge.getEmbeddingModel());
        if (vectorStoreSession != null) {
            vectorStoreSession.close();
        } else {
            closeIfNeeded(knowledge.getEmbeddingStore());
        }
    }

    private static void closeIfNeeded(Object resource) {
        if (resource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 检索已结束，关闭异常不覆盖业务结果。
            }
        }
    }
}
