package com.zw.agent.factory.RAGFactory.vector;

import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

public enum VectorStoreType {
    MILVUS,
    ELASTICSEARCH,
    PGVECTOR,
    QDRANT;

    public static Optional<VectorStoreType> parse(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    valueOf(value.trim().toUpperCase(Locale.ROOT))
            );
        } catch (IllegalArgumentException unsupported) {
            return Optional.empty();
        }
    }
}
