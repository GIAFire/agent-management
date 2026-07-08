package com.zw.agent.factory.ragFactory.backendImpl;

import com.zw.agent.factory.ragFactory.entity.ChunkResult;
import com.zw.agent.factory.ragFactory.entity.KnowledgeBase;
import com.zw.agent.factory.ragFactory.enumeration.BackendStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

public interface KnowledgeBackend {

    BackendStoreType mode();

    List<ChunkResult> indexChunks(
            KnowledgeBase kb,
            List<TextSegment> chunks
    );

    EmbeddingMatch<TextSegment> retrieve(
            KnowledgeBase kb,
            String query,
            int topK,
            double scoreThreshold
    );
}
