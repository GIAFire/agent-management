package com.zw.agent.factory.RAGFactory.vector;

import io.agentscope.core.rag.store.VDBStoreBase;
import java.util.Objects;
import java.util.function.Function;
import reactor.core.publisher.Mono;

public final class VectorStoreSession implements AutoCloseable {

    private final VDBStoreBase store;
    private final Function<String, Mono<Boolean>> documentDeleter;

    public VectorStoreSession(VDBStoreBase store) {
        this(store, store::delete);
    }

    public VectorStoreSession(
            VDBStoreBase store,
            Function<String, Mono<Boolean>> documentDeleter
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.documentDeleter = Objects.requireNonNull(
                documentDeleter,
                "documentDeleter"
        );
    }

    public VDBStoreBase store() {
        return store;
    }

    public Mono<Boolean> deleteDocument(String documentId) {
        return documentDeleter.apply(documentId);
    }

    @Override
    public void close() {
        if (store instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 主操作结果优先，关闭异常不覆盖业务结果。
            }
        }
    }
}
