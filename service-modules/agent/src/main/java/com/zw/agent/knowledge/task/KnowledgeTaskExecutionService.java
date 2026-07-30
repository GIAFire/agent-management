package com.zw.agent.knowledge.task;

import static com.zw.agent.knowledge.KnowledgeConstants.DELETING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_CHUNKING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_EMBEDDING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_FAILED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_INDEXING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_PARSING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_READY;
import static com.zw.agent.knowledge.KnowledgeConstants.ENABLED;
import static com.zw.agent.knowledge.KnowledgeConstants.TASK_DELETE_DOCUMENT;
import static com.zw.agent.knowledge.KnowledgeConstants.TASK_DELETE_KNOWLEDGE_BASE;
import static com.zw.agent.knowledge.KnowledgeConstants.TASK_INDEX_DOCUMENT;
import static com.zw.agent.knowledge.KnowledgeConstants.TASK_RUNNING;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zw.agent.config.knowledge.KnowledgeProperties;
import com.zw.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.entity.AiKnowledgeChunkEntity;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.entity.AiKnowledgeTaskEntity;
import com.zw.agent.factory.RAGFactory.EmbeddingModelFactory;
import com.zw.agent.factory.RAGFactory.MilvusStoreFactory;
import com.zw.agent.knowledge.KnowledgeOperationException;
import com.zw.agent.knowledge.processing.KnowledgeChunker;
import com.zw.agent.knowledge.processing.KnowledgeChunker.ChunkPiece;
import com.zw.agent.knowledge.processing.KnowledgeDocumentParser;
import com.zw.agent.knowledge.storage.KnowledgeSourceStorage;
import com.zw.agent.mapper.AiKnowledgeTaskMapper;
import com.zw.agent.mapper.AiKnowledgeChunkMapper;
import com.zw.agent.service.AiKnowledgeAgentBindingService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.agent.service.AiKnowledgeChunkService;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.store.MilvusStore;
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
public class KnowledgeTaskExecutionService {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeTaskExecutionService.class);

    private final AiKnowledgeTaskMapper taskMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiKnowledgeBaseService knowledgeBaseService;
    private final AiKnowledgeDocumentService documentService;
    private final AiKnowledgeChunkService chunkService;
    private final AiKnowledgeAgentBindingService bindingService;
    private final KnowledgeDocumentParser documentParser;
    private final KnowledgeChunker chunker;
    private final KnowledgeSourceStorage sourceStorage;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final MilvusStoreFactory milvusStoreFactory;
    private final KnowledgeProperties properties;
    private final TransactionTemplate transactionTemplate;

    public void execute(Long taskId, String workerId) {
        AiKnowledgeTaskEntity task = taskMapper.selectWorkerTask(taskId);
        if (task == null
                || !TASK_RUNNING.equals(task.getStatus())
                || !workerId.equals(task.getLeaseOwner())) {
            return;
        }
        UserInfo workerUser = workerUser(task);
        UserContext.runAs(workerUser, () -> executeAsTenant(task, workerId));
    }

    public void cleanExpiredTask(
            AiKnowledgeTaskEntity task,
            String recoveryOwner
    ) {
        UserContext.runAs(
                workerUser(task),
                () -> failTask(
                        task,
                        recoveryOwner,
                        new KnowledgeOperationException(
                                "服务在任务执行期间中断，任务未自动重试"
                        )
                )
        );
    }

    public void rejectBeforeExecution(
            AiKnowledgeTaskEntity task,
            String workerId
    ) {
        UserContext.runAs(
                workerUser(task),
                () -> failTask(
                        task,
                        workerId,
                        new KnowledgeOperationException(
                                "知识任务执行队列已满，请手动重新提交"
                        )
                )
        );
    }

    private void executeAsTenant(AiKnowledgeTaskEntity task, String workerId) {
        try {
            switch (task.getTaskType()) {
                case TASK_INDEX_DOCUMENT -> executeIndexTask(task, workerId);
                case TASK_DELETE_DOCUMENT ->
                        executeDeleteDocumentTask(task, workerId);
                case TASK_DELETE_KNOWLEDGE_BASE ->
                        executeDeleteKnowledgeBaseTask(task, workerId);
                default -> throw new KnowledgeOperationException(
                        "不支持的知识任务类型：" + task.getTaskType()
                );
            }
        } catch (LostKnowledgeTaskLeaseException lostLease) {
            log.warn("Knowledge task stopped after losing lease, taskId={}", task.getId());
        } catch (Throwable error) {
            failTask(task, workerId, error);
        }
    }

    private void executeIndexTask(
            AiKnowledgeTaskEntity task,
            String workerId
    ) {
        AiKnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(task.getKnowledgeBaseId());
        AiKnowledgeDocumentEntity document =
                requireDocument(task.getDocumentId());

        heartbeat(task, workerId, "PARSING", 5, 0, 0);
        updateDocumentStatus(document, DOCUMENT_PARSING, null);
        Path sourcePath = sourceStorage.resolve(document.getSourceUri());
        String extractedText = runWithLeaseHeartbeat(
                task,
                workerId,
                "PARSING",
                5,
                0,
                0,
                () -> documentParser.extractText(
                        sourcePath,
                        document.getDocumentType()
                )
        );

        heartbeat(task, workerId, "CHUNKING", 20, 0, 0);
        updateDocumentStatus(document, DOCUMENT_CHUNKING, null);
        List<ChunkPiece> pieces = chunker.split(
                extractedText,
                task.getChunkStrategy(),
                task.getChunkSize(),
                task.getChunkOverlap(),
                task.getChunkDelimiter()
        );
        List<AiKnowledgeChunkEntity> chunks =
                createChunkEntities(task, document, knowledgeBase, pieces);
        int totalTokens = chunks.stream()
                .map(AiKnowledgeChunkEntity::getTokenCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        heartbeat(task, workerId, "SAVING", 30, 0, chunks.size());
        removeDocumentChunks(document.getId());
        chunkService.saveBatch(chunks, 500);

        updateDocumentStatus(document, DOCUMENT_EMBEDDING, null);
        try (MilvusStore store = milvusStoreFactory.create(knowledgeBase)) {
            // 重提失败索引时先移除可能残留的部分向量。
            store.delete(String.valueOf(document.getId()))
                    .block(Duration.ofMinutes(2));

            EmbeddingModel embeddingModel =
                    embeddingModelFactory.create(knowledgeBase);
            try {
                SimpleKnowledge knowledge = SimpleKnowledge.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(store)
                        .build();
                int batchSize = Math.max(
                        1,
                        properties.getTasks().getEmbeddingBatchSize()
                );
                for (int from = 0; from < chunks.size(); from += batchSize) {
                    int to = Math.min(from + batchSize, chunks.size());
                    heartbeat(
                            task,
                            workerId,
                            "EMBEDDING",
                            35 + (from * 55 / chunks.size()),
                            from,
                            chunks.size()
                    );
                    knowledge.addDocuments(
                                    toVectorDocuments(
                                            chunks.subList(from, to),
                                            document
                                    )
                            )
                            .block(Duration.ofMinutes(10));
                    heartbeat(
                            task,
                            workerId,
                            "EMBEDDING",
                            35 + (to * 55 / chunks.size()),
                            to,
                            chunks.size()
                    );
                }
            } finally {
                closeIfNeeded(embeddingModel);
            }
        }

        heartbeat(
                task,
                workerId,
                "INDEXING",
                95,
                chunks.size(),
                chunks.size()
        );
        updateDocumentStatus(document, DOCUMENT_INDEXING, null);
        heartbeat(
                task,
                workerId,
                "SAVING",
                98,
                chunks.size(),
                chunks.size()
        );

        transactionTemplate.executeWithoutResult(status -> {
            document.setChunkStrategy(task.getChunkStrategy())
                    .setChunkSize(task.getChunkSize())
                    .setChunkOverlap(task.getChunkOverlap())
                    .setChunkDelimiter(task.getChunkDelimiter())
                    .setParseStatus(DOCUMENT_READY)
                    .setStatus(ENABLED)
                    .setChunkCount(chunks.size())
                    .setTokenCount(totalTokens)
                    .setErrorMessage(null);
            EntityDefaults.update(document);
            documentService.updateById(document);
            int completed = taskMapper.completeOwned(
                    task.getId(),
                    task.getTenantId(),
                    workerId,
                    chunks.size(),
                    totalTokens,
                    "{\"documentId\":" + document.getId()
                            + ",\"chunkCount\":" + chunks.size() + "}",
                    LocalDateTime.now()
            );
            ensureOwned(task.getId(), completed);
        });
    }

    private void executeDeleteDocumentTask(
            AiKnowledgeTaskEntity task,
            String workerId
    ) {
        AiKnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(task.getKnowledgeBaseId());
        AiKnowledgeDocumentEntity document =
                requireDocument(task.getDocumentId());

        heartbeat(task, workerId, "DELETING_VECTORS", 15, 0, 3);
        try (MilvusStore store = milvusStoreFactory.create(knowledgeBase)) {
            store.delete(String.valueOf(document.getId()))
                    .block(Duration.ofMinutes(2));
        }

        heartbeat(task, workerId, "DELETING_CHUNKS", 50, 1, 3);
        removeDocumentChunks(document.getId());

        heartbeat(task, workerId, "DELETING_SOURCE", 75, 2, 3);
        try {
            runWithLeaseHeartbeat(
                    task,
                    workerId,
                    "DELETING_SOURCE",
                    75,
                    2,
                    3,
                    () -> {
                        sourceStorage.delete(document.getSourceUri());
                        return null;
                    }
            );
        } catch (Exception error) {
            throw new KnowledgeOperationException("删除知识源文件失败", error);
        }

        heartbeat(task, workerId, "SAVING", 95, 3, 3);
        transactionTemplate.executeWithoutResult(status -> {
            document.setParseStatus(DOCUMENT_DELETED).setStatus(DELETING);
            EntityDefaults.update(document);
            documentService.updateById(document);
            documentService.removeById(document.getId());
            int completed = taskMapper.completeOwned(
                    task.getId(),
                    task.getTenantId(),
                    workerId,
                    0,
                    0,
                    "{\"documentId\":" + document.getId() + "}",
                    LocalDateTime.now()
            );
            ensureOwned(task.getId(), completed);
        });
    }

    private void executeDeleteKnowledgeBaseTask(
            AiKnowledgeTaskEntity task,
            String workerId
    ) {
        AiKnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(task.getKnowledgeBaseId());
        List<AiKnowledgeDocumentEntity> documents =
                documentService.lambdaQuery()
                        .eq(
                                AiKnowledgeDocumentEntity::getKnowledgeBaseId,
                                knowledgeBase.getId()
                        )
                        .list();

        heartbeat(task, workerId, "UNBINDING", 10, 0, 4);
        QueryWrapper<AiKnowledgeAgentBindingEntity> bindingQuery =
                new QueryWrapper<>();
        bindingQuery.eq("knowledge_base_id", knowledgeBase.getId());
        bindingService.remove(bindingQuery);

        heartbeat(task, workerId, "DELETING_VECTORS", 30, 1, 4);
        milvusStoreFactory.dropCollection(knowledgeBase);

        heartbeat(task, workerId, "DELETING_CHUNKS", 55, 2, 4);
        chunkMapper.physicalDeleteByKnowledgeBase(
                task.getTenantId(),
                knowledgeBase.getId()
        );

        int sourceTotal = documents.size();
        heartbeat(task, workerId, "DELETING_SOURCE", 75, 0, sourceTotal);
        for (int index = 0; index < sourceTotal; index++) {
            AiKnowledgeDocumentEntity document = documents.get(index);
            int sourceProgress = 75
                    + (int) (15L * index / Math.max(1, sourceTotal));
            heartbeat(
                    task,
                    workerId,
                    "DELETING_SOURCE",
                    sourceProgress,
                    index,
                    sourceTotal
            );
            try {
                runWithLeaseHeartbeat(
                        task,
                        workerId,
                        "DELETING_SOURCE",
                        sourceProgress,
                        index,
                        sourceTotal,
                        () -> {
                            sourceStorage.delete(document.getSourceUri());
                            return null;
                        }
                );
            } catch (Exception error) {
                throw new KnowledgeOperationException(
                        "删除知识源文件失败：" + document.getDocumentName(),
                        error
                );
            }
        }
        heartbeat(
                task,
                workerId,
                "DELETING_SOURCE",
                90,
                sourceTotal,
                sourceTotal
        );

        heartbeat(task, workerId, "SAVING", 95, 4, 4);
        transactionTemplate.executeWithoutResult(status -> {
            for (AiKnowledgeDocumentEntity document : documents) {
                document.setParseStatus(DOCUMENT_DELETED).setStatus(DELETING);
                EntityDefaults.update(document);
                documentService.updateById(document);
                documentService.removeById(document.getId());
            }
            knowledgeBaseService.removeById(knowledgeBase.getId());
            int completed = taskMapper.completeOwned(
                    task.getId(),
                    task.getTenantId(),
                    workerId,
                    0,
                    0,
                    "{\"knowledgeBaseId\":" + knowledgeBase.getId() + "}",
                    LocalDateTime.now()
            );
            ensureOwned(task.getId(), completed);
        });
    }

    private List<AiKnowledgeChunkEntity> createChunkEntities(
            AiKnowledgeTaskEntity task,
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
                    .setKnowledgeTaskId(task.getId())
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
            chunk.setTenantId(task.getTenantId());
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
                        "knowledgeBaseId",
                        document.getKnowledgeBaseId(),
                        "documentId",
                        document.getId(),
                        "documentName",
                        document.getDocumentName(),
                        "chunkId",
                        chunk.getId(),
                        "chunkIndex",
                        chunk.getChunkIndex()
                )
        );
        return new Document(metadata);
    }

    private void failTask(
            AiKnowledgeTaskEntity task,
            String workerId,
            Throwable error
    ) {
        String reason = failureReason(task, error);
        log.warn(
                "Knowledge task failed, taskId={}, taskType={}, errorType={}",
                task.getId(),
                task.getTaskType(),
                error.getClass().getSimpleName()
        );

        try {
            heartbeat(
                    task,
                    workerId,
                    "CLEANING_UP",
                    task.getProgress() == null ? 1 : task.getProgress(),
                    task.getCompletedUnits() == null
                            ? 0
                            : task.getCompletedUnits(),
                    task.getTotalUnits() == null ? 0 : task.getTotalUnits()
            );
        } catch (LostKnowledgeTaskLeaseException ignored) {
            log.warn(
                    "Knowledge task cleanup skipped because lease was lost, taskId={}",
                    task.getId()
            );
            return;
        }

        if (TASK_INDEX_DOCUMENT.equals(task.getTaskType())) {
            cleanupIndexArtifacts(task);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                int failed = taskMapper.failOwned(
                        task.getId(),
                        task.getTenantId(),
                        workerId,
                        reason,
                        LocalDateTime.now()
                );
                ensureOwned(task.getId(), failed);
                if (TASK_INDEX_DOCUMENT.equals(task.getTaskType())) {
                    AiKnowledgeDocumentEntity document =
                            documentService.getById(task.getDocumentId());
                    if (document != null) {
                        document.setParseStatus(DOCUMENT_FAILED)
                                .setStatus(ENABLED)
                                .setChunkCount(0)
                                .setTokenCount(0)
                                .setErrorMessage(reason);
                        EntityDefaults.update(document);
                        documentService.updateById(document);
                    }
                } else if (TASK_DELETE_DOCUMENT.equals(task.getTaskType())) {
                    markDocumentDeleteFailed(task, reason);
                }
                // 删除知识库失败时保持 status=DELETING，等待用户手动重提。
            });
        } catch (LostKnowledgeTaskLeaseException ignored) {
            log.warn(
                    "Knowledge task failure was not recorded because lease was lost, taskId={}",
                    task.getId()
            );
        }
    }

    private void markDocumentDeleteFailed(
            AiKnowledgeTaskEntity task,
            String reason
    ) {
        AiKnowledgeDocumentEntity document =
                documentService.getById(task.getDocumentId());
        if (document == null) {
            return;
        }
        document.setParseStatus(
                        com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETING
                )
                .setStatus(DELETING)
                .setErrorMessage(reason);
        EntityDefaults.update(document);
        documentService.updateById(document);
    }

    private void cleanupIndexArtifacts(AiKnowledgeTaskEntity task) {
        try {
            AiKnowledgeBaseEntity knowledgeBase =
                    knowledgeBaseService.getById(task.getKnowledgeBaseId());
            if (knowledgeBase != null) {
                try (MilvusStore store = milvusStoreFactory.create(knowledgeBase)) {
                    store.delete(String.valueOf(task.getDocumentId()))
                            .block(Duration.ofMinutes(2));
                }
            }
        } catch (Throwable cleanupError) {
            log.warn(
                    "Failed to clean vector artifacts, taskId={}, errorType={}",
                    task.getId(),
                    cleanupError.getClass().getSimpleName()
            );
        }
        try {
            removeDocumentChunks(task.getDocumentId());
        } catch (Throwable cleanupError) {
            log.warn(
                    "Failed to clean chunk artifacts, taskId={}, errorType={}",
                    task.getId(),
                    cleanupError.getClass().getSimpleName()
            );
        }
    }

    private void removeDocumentChunks(Long documentId) {
        Long tenantId = UserContext.get() == null
                ? null
                : UserContext.get().getTenantId();
        if (tenantId == null) {
            throw new KnowledgeOperationException("知识任务缺少租户上下文");
        }
        chunkMapper.physicalDeleteByDocument(tenantId, documentId);
    }

    private void heartbeat(
            AiKnowledgeTaskEntity task,
            String workerId,
            String stage,
            int progress,
            int completedUnits,
            int totalUnits
    ) {
        LocalDateTime now = LocalDateTime.now();
        int updated = taskMapper.heartbeat(
                task.getId(),
                task.getTenantId(),
                workerId,
                stage,
                Math.max(0, Math.min(99, progress)),
                completedUnits,
                totalUnits,
                now.plus(properties.getTasks().getLeaseDuration()),
                now
        );
        ensureOwned(task.getId(), updated);
    }

    private <T> T runWithLeaseHeartbeat(
            AiKnowledgeTaskEntity task,
            String workerId,
            String stage,
            int progress,
            int completedUnits,
            int totalUnits,
            Callable<T> operation
    ) {
        FutureTask<T> future = new FutureTask<>(operation);
        Thread.ofVirtual()
                .name("knowledge-blocking-" + task.getId())
                .start(future);
        try {
            while (true) {
                try {
                    return future.get(
                            leaseHeartbeatIntervalMillis(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (TimeoutException ignored) {
                    heartbeat(
                            task,
                            workerId,
                            stage,
                            progress,
                            completedUnits,
                            totalUnits
                    );
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new KnowledgeOperationException(
                    "知识任务执行被中断",
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
                    "知识任务执行失败",
                    cause
            );
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private long leaseHeartbeatIntervalMillis() {
        long leaseMillis = properties.getTasks()
                .getLeaseDuration()
                .toMillis();
        return Math.max(
                50L,
                Math.min(Duration.ofSeconds(30).toMillis(), leaseMillis / 3L)
        );
    }

    private void updateDocumentStatus(
            AiKnowledgeDocumentEntity document,
            String parseStatus,
            String errorMessage
    ) {
        document.setParseStatus(parseStatus).setErrorMessage(errorMessage);
        EntityDefaults.update(document);
        documentService.updateById(document);
    }

    private AiKnowledgeBaseEntity requireKnowledgeBase(Long knowledgeBaseId) {
        AiKnowledgeBaseEntity entity = knowledgeBaseService.getById(knowledgeBaseId);
        if (entity == null) {
            throw new KnowledgeOperationException("知识库不存在");
        }
        return entity;
    }

    private AiKnowledgeDocumentEntity requireDocument(Long documentId) {
        AiKnowledgeDocumentEntity entity = documentService.getById(documentId);
        if (entity == null) {
            throw new KnowledgeOperationException("知识文档不存在");
        }
        return entity;
    }

    private String failureReason(
            AiKnowledgeTaskEntity task,
            Throwable error
    ) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = StringUtils.hasText(root.getMessage())
                ? root.getMessage()
                : "任务执行失败";
        AiKnowledgeBaseEntity knowledgeBase =
                knowledgeBaseService.getById(task.getKnowledgeBaseId());
        if (knowledgeBase != null && StringUtils.hasText(knowledgeBase.getApiKey())) {
            message = message.replace(knowledgeBase.getApiKey(), "***");
        }
        if (message.length() > 1800) {
            message = message.substring(0, 1800);
        }
        return message;
    }

    private static UserInfo workerUser(AiKnowledgeTaskEntity task) {
        UserInfo user = new UserInfo();
        user.setTenantId(task.getTenantId());
        user.setUserId(task.getCreatedBy() == null ? 0L : task.getCreatedBy());
        user.setUserName("knowledge-task-worker");
        user.setStatus((byte) 1);
        return user;
    }

    private static void ensureOwned(Long taskId, int affectedRows) {
        if (affectedRows != 1) {
            throw new LostKnowledgeTaskLeaseException(taskId);
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
                // 主操作结果优先，连接池关闭失败不会改写任务结果。
            }
        }
    }
}
