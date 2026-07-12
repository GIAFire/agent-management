package com.zw.agent.factory.RAGFactory.backendImpl;

import com.zw.agent.factory.RAGFactory.entity.ChunkResult;
import com.zw.agent.factory.RAGFactory.entity.KnowledgeBase;
import com.zw.agent.factory.RAGFactory.enumeration.BackendStoreType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SelfManagedKnowledgeBackend implements KnowledgeBackend {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public BackendStoreType mode() {
        return BackendStoreType.SELF_MANAGED;
    }

    @Override
    public List<ChunkResult> indexChunks(KnowledgeBase kb, List<TextSegment> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        List<Embedding> embeddings = embeddingModel.embedAll(chunks).content();
        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding result size does not match chunk size");
        }

        List<String> vectorIds = chunks.stream()
                .map(chunk -> chunk.metadata().getString("chunk_uid"))
                .filter(Objects::nonNull)
                .toList();
        if (vectorIds.size() != chunks.size()) {
            vectorIds = embeddingStore.generateIds(chunks.size());
        }

        embeddingStore.addAll(vectorIds, embeddings, chunks);

        List<String> storedVectorIds = vectorIds;
        return IntStream.range(0, storedVectorIds.size())
                .mapToObj(index -> {
                    ChunkResult result = new ChunkResult();
                    result.setVectorId(storedVectorIds.get(index));
                    return result;
                })
                .toList();
    }

    @Override
    public EmbeddingMatch<TextSegment> retrieve(KnowledgeBase kb, String query, int topK, double scoreThreshold) {
        List<EmbeddingMatch<TextSegment>> matches = retrieveMatches(kb, query, topK, scoreThreshold);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.getFirst();
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> retrieveMatches(KnowledgeBase kb, String query, int topK, double scoreThreshold) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(scoreThreshold)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches();
    }
}
