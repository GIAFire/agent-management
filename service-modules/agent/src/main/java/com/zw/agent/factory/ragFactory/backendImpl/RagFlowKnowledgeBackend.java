package com.zw.agent.factory.ragFactory.backendImpl;

import com.zw.agent.factory.ragFactory.entity.ChunkResult;
import com.zw.agent.factory.ragFactory.entity.KnowledgeBase;
import com.zw.agent.factory.ragFactory.enumeration.BackendStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

public class RagFlowKnowledgeBackend implements KnowledgeBackend {

    private final RagFlowClient ragFlowClient;

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

    // 调 RAGFlow 上传文档、解析、检索接口
}