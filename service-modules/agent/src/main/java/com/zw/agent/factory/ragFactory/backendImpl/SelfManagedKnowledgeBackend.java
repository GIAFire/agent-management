package com.zw.agent.factory.ragFactory.backendImpl;

import com.zw.agent.factory.ragFactory.entity.ChunkResult;
import com.zw.agent.factory.ragFactory.entity.KnowledgeBase;
import com.zw.agent.factory.ragFactory.enumeration.BackendStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SelfManagedKnowledgeBackend implements KnowledgeBackend {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public BackendStoreType mode() {
        return null;
    }

    @Override
    public List<ChunkResult> indexChunks(KnowledgeBase kb, List<TextSegment> chunks) {
        return null;
    }

    @Override
    public EmbeddingMatch<TextSegment> retrieve(KnowledgeBase kb, String query, int topK, double scoreThreshold) {
        return null;
    }

    // 批量 embedding + 批量写入 Qdrant/Milvus/ES
}
