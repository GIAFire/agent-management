package com.zhiran.agent.knowledge.processing;

import static com.zhiran.agent.knowledge.KnowledgeConstants.DELETING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.DOCUMENT_CHUNKING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.DOCUMENT_EMBEDDING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.DOCUMENT_INDEXING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.DOCUMENT_PARSING;
import static com.zhiran.agent.knowledge.KnowledgeConstants.ENABLED;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zhiran.agent.config.knowledge.KnowledgeProperties;
import com.zhiran.agent.entity.AiKnowledgeBaseEntity;
import com.zhiran.agent.entity.AiKnowledgeChunkEntity;
import com.zhiran.agent.entity.AiKnowledgeDocumentEntity;
import com.zhiran.agent.factory.RAGFactory.EmbeddingModelFactory;
import com.zhiran.agent.factory.RAGFactory.VectorStoreFactory;
import com.zhiran.agent.factory.RAGFactory.vector.VectorStoreSession;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import com.zhiran.agent.knowledge.processing.KnowledgeChunker.ChunkPiece;
import com.zhiran.agent.knowledge.storage.KnowledgeSourceStorage;
import com.zhiran.agent.mapper.AiKnowledgeChunkMapper;
import com.zhiran.agent.mapper.AiKnowledgeDocumentMapper;
import com.zhiran.agent.service.AiKnowledgeBaseService;
import com.zhiran.agent.service.AiKnowledgeChunkService;
import com.zhiran.common.context.UserContext;
import com.zhiran.common.context.UserInfo;
import com.zhiran.common.support.EntityDefaults;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeDocumentProcessingService.class);

    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiKnowledgeBaseService knowledgeBaseService;
    private final AiKnowledgeChunkService chunkService;
    private final KnowledgeDocumentParser documentParser;
    private final KnowledgeChunker chunker;
    private final KnowledgeSourceStorage sourceStorage;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final VectorStoreFactory vectorStoreFactory;
    private final KnowledgeProperties properties;
    private final TransactionTemplate transactionTemplate;

    public void execute(Long documentId, String workerId) {
        AiKnowledgeDocumentEntity document =
                documentMapper.selectWorkerDocument(documentId);
        if (document == null
                || !workerId.equals(document.getLeaseOwner())) {
            return;
        }
        UserContext.runAs(
                workerUser(document),
                () -> executeAsTenant(document, workerId)
        );
    }

    public void cleanExpiredDocument(
            AiKnowledgeDocumentEntity document,
            String recoveryOwner
    ) {
        UserContext.runAs(
                workerUser(document),
                () -> failProcessing(
                        document,
                        recoveryOwner,
                        new KnowledgeOperationException(
                                "服务在知识文档处理期间中断，请手动重试"
                        )
                )
        );
    }

    public void rejectBeforeExecution(
            AiKnowledgeDocumentEntity document,
            String workerId
    ) {
        UserContext.runAs(
                workerUser(document),
                () -> failProcessing(
                        document,
                        workerId,
                        new KnowledgeOperationException(
                                "知识文档处理队列已满，请手动重试"
                        )
                )
        );
    }

    private void executeAsTenant(
            AiKnowledgeDocumentEntity document,
            String workerId
    ) {
        try {
            if (isDeleteOperation(document)) {
                executeDelete(document, workerId);
            } else {
                executeIndex(document, workerId);
            }
        } catch (LostKnowledgeDocumentLeaseException lostLease) {
            log.warn(
                    "Knowledge document processing stopped after losing lease, documentId={}",
                    document.getId()
            );
        } catch (Throwable error) {
            failProcessing(document, workerId, error);
        }
    }

    private void executeIndex(
            AiKnowledgeDocumentEntity document,
            String workerId
    ) {
        AiKnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(document.getKnowledgeBaseId());

        heartbeat(document, workerId, DOCUMENT_PARSING);
        Path sourcePath = sourceStorage.resolve(document.getSourceUri());
        String extractedText = runWithLeaseHeartbeat(
                document,
                workerId,
                DOCUMENT_PARSING,
                () -> documentParser.extractText(
                        sourcePath,
                        document.getDocumentType()
                )
        );

        heartbeat(document, workerId, DOCUMENT_CHUNKING);
        List<ChunkPiece> pieces = chunker.split(
                extractedText,
                document.getChunkStrategy(),
                document.getChunkSize(),
                document.getChunkOverlap(),
                document.getChunkDelimiter()
        );
        List<AiKnowledgeChunkEntity> chunks =
                createChunkEntities(document, knowledgeBase, pieces);
        int totalTokens = chunks.stream()
                .map(AiKnowledgeChunkEntity::getTokenCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        removeDocumentChunks(document.getId());
        chunkService.saveBatch(chunks, 500);

        heartbeat(document, workerId, DOCUMENT_EMBEDDING);
        try (VectorStoreSession session = vectorStoreFactory.create(knowledgeBase)) {
            session.deleteDocument(String.valueOf(document.getId()))
                    .block(Duration.ofMinutes(2));

            EmbeddingModel embeddingModel =
                    embeddingModelFactory.create(knowledgeBase);
            try {
                SimpleKnowledge knowledge = SimpleKnowledge.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(session.store())
                        .build();
                int batchSize = Math.max(
                        1,
                        properties.getProcessing().getEmbeddingBatchSize()
                );
                for (int from = 0; from < chunks.size(); from += batchSize) {
                    int to = Math.min(from + batchSize, chunks.size());
                    heartbeat(document, workerId, DOCUMENT_EMBEDDING);
                    knowledge.addDocuments(
                                    toVectorDocuments(
                                            chunks.subList(from, to),
                                            document
                                    )
                            )
                            .block(Duration.ofMinutes(10));
                }
            } finally {
                closeIfNeeded(embeddingModel);
            }
        }

        heartbeat(document, workerId, DOCUMENT_INDEXING);
        transactionTemplate.executeWithoutResult(status -> {
            int completed = documentMapper.completeIndexOwned(
                    document.getId(),
                    document.getTenantId(),
                    workerId,
                    chunks.size(),
                    totalTokens,
                    LocalDateTime.now()
            );
            ensureOwned(document.getId(), completed);
        });
    }

    private void executeDelete(
            AiKnowledgeDocumentEntity document,
            String workerId
    ) {
        AiKnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(document.getKnowledgeBaseId());

        heartbeat(document, workerId, DOCUMENT_DELETING);
        try (VectorStoreSession session = vectorStoreFactory.create(knowledgeBase)) {
            session.deleteDocument(String.valueOf(document.getId()))
                    .block(Duration.ofMinutes(2));
        }

        heartbeat(document, workerId, DOCUMENT_DELETING);
        removeDocumentChunks(document.getId());

        try {
            runWithLeaseHeartbeat(
                    document,
                    workerId,
                    DOCUMENT_DELETING,
                    () -> {
                        sourceStorage.delete(document.getSourceUri());
                        return null;
                    }
            );
        } catch (Exception error) {
            throw new KnowledgeOperationException("删除知识源文件失败", error);
        }

        heartbeat(document, workerId, DOCUMENT_DELETING);
        transactionTemplate.executeWithoutResult(status -> {
            int completed = documentMapper.completeDeleteOwned(
                    document.getId(),
                    document.getTenantId(),
                    workerId,
                    LocalDateTime.now()
            );
            ensureOwned(document.getId(), completed);
        });
    }

    private List<AiKnowledgeChunkEntity> createChunkEntities(
            AiKnowledgeDocumentEntity document,
            AiKnowledgeBaseEntity knowledgeBase,
            List<ChunkPiece> pieces
    ) {
        List<AiKnowledgeChunkEntity> chunks = new ArrayList<>(pieces.size());
        for (int index = 0; index < pieces.size(); index++) {
            ChunkPiece piece = pieces.get(index);
            String contentHash = sha256(piece.content());
            String chunkUid = sha256(
                    document.getId() + ":" + index + ":" + contentHash
            );
            AiKnowledgeChunkEntity chunk = new AiKnowledgeChunkEntity()
                    .setId(IdWorker.getId())
                    .setKnowledgeBaseId(knowledgeBase.getId())
                    .setDocumentId(document.getId())
                    .setChunkIndex(index)
                    .setChunkUid(chunkUid)
                    .setExternalChunkId(String.valueOf(index))
                    .setContent(piece.content())
                    .setContentHash(contentHash)
                    .setContentType("TEXT")
                    .setStartOffset(piece.startOffset())
                    .setEndOffset(piece.endOffset())
                    .setTokenCount(estimateTokens(piece.content()))
                    .setEmbeddingDimension(knowledgeBase.getEmbeddingDimension())
                    .setStatus(ENABLED);
            chunk.setTenantId(document.getTenantId());
            Document vectorDocument = toVectorDocument(chunk, document);
            chunk.setVectorId(vectorDocument.getId());
            EntityDefaults.create(chunk);
            chunks.add(chunk);
        }
        return chunks;
    }

    private List<Document> toVectorDocuments(
            List<AiKnowledgeChunkEntity> chunks,
            AiKnowledgeDocumentEntity document
    ) {
        return chunks.stream()
                .map(chunk -> toVectorDocument(chunk, document))
                .toList();
    }

    private Document toVectorDocument(
            AiKnowledgeChunkEntity chunk,
            AiKnowledgeDocumentEntity document
    ) {
        DocumentMetadata metadata = new DocumentMetadata(
                TextBlock.builder().text(chunk.getContent()).build(),
                String.valueOf(document.getId()),
                String.valueOf(chunk.getChunkIndex()),
                Map.of(
                        "knowledgeBaseId", document.getKnowledgeBaseId(),
                        "documentId", document.getId(),
                        "documentName", document.getDocumentName(),
                        "chunkId", chunk.getId(),
                        "chunkIndex", chunk.getChunkIndex()
                )
        );
        return new Document(metadata);
    }

    private void failProcessing(
            AiKnowledgeDocumentEntity document,
            String workerId,
            Throwable error
    ) {
        boolean deleting = isDeleteOperation(document);
        String reason = failureReason(document, error);
        log.warn(
                "Knowledge document processing failed, documentId={}, operation={}, errorType={}",
                document.getId(),
                deleting ? "DELETE" : "INDEX",
                error.getClass().getSimpleName()
        );

        try {
            heartbeat(
                    document,
                    workerId,
                    deleting ? DOCUMENT_DELETING : document.getParseStatus()
            );
        } catch (LostKnowledgeDocumentLeaseException ignored) {
            log.warn(
                    "Knowledge document cleanup skipped because lease was lost, documentId={}",
                    document.getId()
            );
            return;
        }

        if (!deleting) {
            cleanupIndexArtifacts(document);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                int failed = deleting
                        ? documentMapper.failDeleteOwned(
                                document.getId(),
                                document.getTenantId(),
                                workerId,
                                reason,
                                now
                        )
                        : documentMapper.failIndexOwned(
                                document.getId(),
                                document.getTenantId(),
                                workerId,
                                reason,
                                now
                        );
                ensureOwned(document.getId(), failed);
            });
        } catch (LostKnowledgeDocumentLeaseException ignored) {
            log.warn(
                    "Knowledge document failure was not recorded because lease was lost, documentId={}",
                    document.getId()
            );
        }
    }

    private void cleanupIndexArtifacts(AiKnowledgeDocumentEntity document) {
        try {
            AiKnowledgeBaseEntity knowledgeBase =
                    knowledgeBaseService.getById(document.getKnowledgeBaseId());
            if (knowledgeBase != null) {
                try (VectorStoreSession session = vectorStoreFactory.create(knowledgeBase)) {
                    session.deleteDocument(String.valueOf(document.getId()))
                            .block(Duration.ofMinutes(2));
                }
            }
        } catch (Throwable cleanupError) {
            log.warn(
                    "Failed to clean vector artifacts, documentId={}, errorType={}",
                    document.getId(),
                    cleanupError.getClass().getSimpleName()
            );
        }
        try {
            removeDocumentChunks(document.getId());
        } catch (Throwable cleanupError) {
            log.warn(
                    "Failed to clean chunk artifacts, documentId={}, errorType={}",
                    document.getId(),
                    cleanupError.getClass().getSimpleName()
            );
        }
    }

    private void removeDocumentChunks(Long documentId) {
        Long tenantId = UserContext.get() == null
                ? null
                : UserContext.get().getTenantId();
        if (tenantId == null) {
            throw new KnowledgeOperationException("知识文档处理缺少租户上下文");
        }
        chunkMapper.physicalDeleteByDocument(tenantId, documentId);
    }

    private void heartbeat(
            AiKnowledgeDocumentEntity document,
            String workerId,
            String parseStatus
    ) {
        LocalDateTime now = LocalDateTime.now();
        int updated = documentMapper.heartbeat(
                document.getId(),
                document.getTenantId(),
                workerId,
                parseStatus,
                now.plus(properties.getProcessing().getLeaseDuration()),
                now
        );
        ensureOwned(document.getId(), updated);
        document.setParseStatus(parseStatus);
    }

    private <T> T runWithLeaseHeartbeat(
            AiKnowledgeDocumentEntity document,
            String workerId,
            String parseStatus,
            Callable<T> operation
    ) {
        FutureTask<T> future = new FutureTask<>(operation);
        Thread.ofVirtual()
                .name("knowledge-blocking-" + document.getId())
                .start(future);
        try {
            while (true) {
                try {
                    return future.get(
                            leaseHeartbeatIntervalMillis(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (TimeoutException ignored) {
                    heartbeat(document, workerId, parseStatus);
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new KnowledgeOperationException(
                    "知识文档处理被中断",
                    error
            );
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeError) {
                throw runtimeError;
            }
            if (cause instanceof Error fatalError) {
                throw fatalError;
            }
            throw new KnowledgeOperationException(
                    "知识文档处理失败",
                    cause
            );
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private long leaseHeartbeatIntervalMillis() {
        long leaseMillis = properties.getProcessing()
                .getLeaseDuration()
                .toMillis();
        return Math.max(
                50L,
                Math.min(Duration.ofSeconds(30).toMillis(), leaseMillis / 3L)
        );
    }

    private AiKnowledgeBaseEntity requireKnowledgeBase(Long knowledgeBaseId) {
        AiKnowledgeBaseEntity entity = knowledgeBaseService.getById(knowledgeBaseId);
        if (entity == null) {
            throw new KnowledgeOperationException("知识库不存在");
        }
        return entity;
    }

    private String failureReason(
            AiKnowledgeDocumentEntity document,
            Throwable error
    ) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = StringUtils.hasText(root.getMessage())
                ? root.getMessage()
                : "知识文档处理失败";
        AiKnowledgeBaseEntity knowledgeBase =
                knowledgeBaseService.getById(document.getKnowledgeBaseId());
        if (knowledgeBase != null && StringUtils.hasText(knowledgeBase.getApiKey())) {
            message = message.replace(knowledgeBase.getApiKey(), "***");
        }
        if (message.length() > 1800) {
            message = message.substring(0, 1800);
        }
        return message;
    }

    private static boolean isDeleteOperation(
            AiKnowledgeDocumentEntity document
    ) {
        return Objects.equals(document.getStatus(), DELETING)
                && DOCUMENT_DELETING.equals(document.getParseStatus());
    }

    private static UserInfo workerUser(AiKnowledgeDocumentEntity document) {
        UserInfo user = new UserInfo();
        user.setTenantId(document.getTenantId());
        user.setUserId(document.getCreatedBy() == null ? 0L : document.getCreatedBy());
        user.setUserName("knowledge-document-worker");
        user.setStatus((byte) 1);
        return user;
    }

    private static void ensureOwned(Long documentId, int affectedRows) {
        if (affectedRows != 1) {
            throw new LostKnowledgeDocumentLeaseException(documentId);
        }
    }

    private static int estimateTokens(String content) {
        return Math.max(1, (content.length() + 3) / 4);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM 不支持 SHA-256", impossible);
        }
    }

    private static void closeIfNeeded(Object resource) {
        if (resource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 主操作结果优先，连接池关闭失败不会改写处理结果。
            }
        }
    }
}
