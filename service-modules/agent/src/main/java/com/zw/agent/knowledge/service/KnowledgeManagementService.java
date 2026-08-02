package com.zw.agent.knowledge.service;

import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_CHARACTER;
import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_DELIMITER;
import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_PARAGRAPH;
import static com.zw.agent.knowledge.KnowledgeConstants.DEFAULT_CHUNK_OVERLAP;
import static com.zw.agent.knowledge.KnowledgeConstants.DEFAULT_CHUNK_SIZE;
import static com.zw.agent.knowledge.KnowledgeConstants.DELETING;
import static com.zw.agent.knowledge.KnowledgeConstants.DISABLED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_CHUNKING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETE_FAILED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_EMBEDDING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_FAILED;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_INDEXING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_PARSING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_PENDING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_READY;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_UPLOADED;
import static com.zw.agent.knowledge.KnowledgeConstants.ENABLED;
import static com.zw.agent.knowledge.KnowledgeConstants.MAX_CHUNK_OVERLAP;
import static com.zw.agent.knowledge.KnowledgeConstants.MAX_CHUNK_SIZE;
import static com.zw.agent.knowledge.KnowledgeConstants.MAX_DELIMITER_LENGTH;
import static com.zw.agent.knowledge.KnowledgeConstants.MIN_CHUNK_SIZE;
import static com.zw.agent.knowledge.KnowledgeConstants.SUPPORTED_METRICS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeAgentBindingEntity;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.entity.AiKnowledgeChunkEntity;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.factory.RAGFactory.EmbeddingModelFactory;
import com.zw.agent.factory.RAGFactory.MilvusStoreFactory;
import com.zw.agent.constant.enumeration.ApiType;
import com.zw.agent.knowledge.KnowledgeOperationException;
import com.zw.agent.knowledge.dto.KnowledgeBaseCreateRequest;
import com.zw.agent.knowledge.dto.KnowledgeBaseOptionResponse;
import com.zw.agent.knowledge.dto.KnowledgeBaseResponse;
import com.zw.agent.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.zw.agent.knowledge.dto.KnowledgeChunkResponse;
import com.zw.agent.knowledge.dto.KnowledgeDocumentResponse;
import com.zw.agent.knowledge.dto.KnowledgeIndexRequest;
import com.zw.agent.knowledge.dto.KnowledgeMetricsResponse;
import com.zw.agent.knowledge.processing.KnowledgeDocumentDispatcher;
import com.zw.agent.knowledge.processing.KnowledgeUploadValidator;
import com.zw.agent.knowledge.processing.KnowledgeUploadValidator.ValidatedUpload;
import com.zw.agent.knowledge.storage.KnowledgeSourceStorage;
import com.zw.agent.knowledge.storage.KnowledgeSourceStorage.StoredSource;
import com.zw.agent.mapper.AiKnowledgeBaseMapper;
import com.zw.agent.mapper.AiKnowledgeChunkMapper;
import com.zw.agent.mapper.AiKnowledgeDocumentMapper;
import com.zw.agent.service.AiKnowledgeAgentBindingService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.agent.service.AiKnowledgeChunkService;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.rag.store.MilvusStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class KnowledgeManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeManagementService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final AiKnowledgeBaseService knowledgeBaseService;
    private final AiKnowledgeDocumentService documentService;
    private final AiKnowledgeChunkService chunkService;
    private final AiKnowledgeAgentBindingService bindingService;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final MilvusStoreFactory milvusStoreFactory;
    private final KnowledgeUploadValidator uploadValidator;
    private final KnowledgeSourceStorage sourceStorage;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeDocumentDispatcher documentDispatcher;

    public KnowledgeMetricsResponse knowledgeMetrics() {
        Long tenantId = currentUser().getTenantId();
        LambdaQueryWrapper<AiKnowledgeBaseEntity> activeBases =
                new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                        .eq(AiKnowledgeBaseEntity::getTenantId, tenantId)
                        .ne(AiKnowledgeBaseEntity::getStatus, DELETING)
                        .eq(AiKnowledgeBaseEntity::getDeleted, 0);
        long totalBases = knowledgeBaseService.count(activeBases);
        long enabledBases = knowledgeBaseService.count(
                new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                        .eq(AiKnowledgeBaseEntity::getTenantId, tenantId)
                        .eq(AiKnowledgeBaseEntity::getStatus, ENABLED)
                        .eq(AiKnowledgeBaseEntity::getDeleted, 0));
        List<AiKnowledgeDocumentEntity> documents = documentService.list(
                new LambdaQueryWrapper<AiKnowledgeDocumentEntity>()
                        .eq(AiKnowledgeDocumentEntity::getTenantId, tenantId)
                        .eq(AiKnowledgeDocumentEntity::getDeleted, 0)
                        .ne(AiKnowledgeDocumentEntity::getStatus, DELETING));
        long ready = documents.stream()
                .filter(item -> DOCUMENT_READY.equals(item.getParseStatus()))
                .count();
        long newToday = documents.stream()
                .filter(item -> item.getCreatedAt() != null
                        && !item.getCreatedAt().isBefore(LocalDate.now().atStartOfDay()))
                .count();
        long chunks = documents.stream().map(AiKnowledgeDocumentEntity::getChunkCount)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long tokens = documents.stream().map(AiKnowledgeDocumentEntity::getTokenCount)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        Double readyRate = documents.isEmpty()
                ? null : Math.round(ready * 10000D / documents.size()) / 100D;
        return new KnowledgeMetricsResponse(
                totalBases, enabledBases, documents.size(), newToday,
                ready, readyRate, chunks, tokens);
    }

    public IPage<KnowledgeBaseResponse> pageKnowledgeBases(
            long current,
            long size,
            String keyword,
            Byte status
    ) {
        currentUser();
        QueryWrapper<AiKnowledgeBaseEntity> query = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.like("knowledge_name", keyword.trim());
        }
        if (status != null) {
            query.eq("status", status);
        }
        query.orderByDesc("updated_at").orderByDesc("created_at");
        return knowledgeBaseService
                .page(new Page<>(positive(current), pageSize(size)), query)
                .convert(this::toKnowledgeBaseResponse);
    }

    public KnowledgeBaseResponse getKnowledgeBase(Long knowledgeBaseId) {
        return toKnowledgeBaseResponse(requireKnowledgeBase(knowledgeBaseId));
    }

    public List<KnowledgeBaseOptionResponse> listKnowledgeBaseOptions() {
        currentUser();
        return knowledgeBaseService.lambdaQuery()
                .eq(AiKnowledgeBaseEntity::getStatus, ENABLED)
                .orderByAsc(AiKnowledgeBaseEntity::getKnowledgeName)
                .list()
                .stream()
                .map(entity -> new KnowledgeBaseOptionResponse(
                        entity.getId(),
                        entity.getKnowledgeName(),
                        entity.getStatus()
                ))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseResponse createKnowledgeBase(
            KnowledgeBaseCreateRequest request
    ) {
        UserInfo user = currentUser();
        validateCreateRequest(request);
        String knowledgeName = normalizeRequired(request.getKnowledgeName(), "知识库名称");
        assertKnowledgeBaseNameAvailable(knowledgeName, null);

        Long id = IdWorker.getId();
        AiKnowledgeBaseEntity entity = new AiKnowledgeBaseEntity()
                .setId(id)
                .setUserId(user.getUserId())
                .setKnowledgeName(knowledgeName)
                .setKnowledgeCode("kb_" + id)
                .setCollectionName(
                        "kb_t" + user.getTenantId() + "_" + id
                )
                .setDescription(trimToNull(request.getDescription()))
                .setRerankEnabled((byte) 0)
                .setStatus(ENABLED)
                .setBackendStoreType("MILVUS")
                .setApiType(ApiType.OPENAI)
                .setModelUrl(request.getModelUrl().trim())
                .setApiKey(request.getApiKey().trim())
                .setEmbeddingModelName(request.getEmbeddingModelName().trim())
                .setEmbeddingDimension(request.getEmbeddingDimension())
                .setMetricType(normalizeMetric(request.getMetricType()))
                .setTopK(defaultTopK(request.getTopK()))
                .setScoreThreshold(defaultThreshold(request.getScoreThreshold()));
        entity.setTenantId(user.getTenantId());
        EntityDefaults.create(entity);

        validateEmbeddingConnection(entity);
        boolean collectionCreated = false;
        try {
            try (MilvusStore ignored = milvusStoreFactory.create(entity)) {
                collectionCreated = true;
            }
            knowledgeBaseService.save(entity);
            return toKnowledgeBaseResponse(entity);
        } catch (RuntimeException error) {
            if (collectionCreated) {
                try {
                    milvusStoreFactory.dropCollection(entity);
                } catch (RuntimeException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }
            if (error instanceof DuplicateKeyException) {
                throw new KnowledgeOperationException("同名知识库已存在", error);
            }
            if (error instanceof KnowledgeOperationException operationError) {
                throw operationError;
            }
            throw new KnowledgeOperationException(
                    "创建知识库失败：" + safeMessage(error, entity.getApiKey()),
                    error
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseResponse updateKnowledgeBase(
            Long knowledgeBaseId,
            KnowledgeBaseUpdateRequest request
    ) {
        if (request == null) {
            throw new KnowledgeOperationException("知识库更新参数不能为空");
        }
        AiKnowledgeBaseEntity entity =
                requireKnowledgeBaseForUpdate(knowledgeBaseId);
        if (Objects.equals(entity.getStatus(), DELETING)) {
            throw new KnowledgeOperationException("知识库删除中，不能修改");
        }

        String name = normalizeRequired(request.getKnowledgeName(), "知识库名称");
        assertKnowledgeBaseNameAvailable(name, knowledgeBaseId);
        String modelUrl = normalizeRequired(request.getModelUrl(), "Embedding URL");
        int topK = defaultTopK(request.getTopK());
        BigDecimal threshold = defaultThreshold(request.getScoreThreshold());
        if (request.getStatus() != null
                && request.getStatus() != ENABLED
                && request.getStatus() != DISABLED) {
            throw new KnowledgeOperationException("知识库状态只能为启用或停用");
        }

        boolean embeddingChanged = !modelUrl.equals(entity.getModelUrl())
                || StringUtils.hasText(request.getApiKey());
        entity.setKnowledgeName(name)
                .setDescription(trimToNull(request.getDescription()))
                .setModelUrl(modelUrl)
                .setTopK(topK)
                .setScoreThreshold(threshold);
        if (StringUtils.hasText(request.getApiKey())) {
            entity.setApiKey(request.getApiKey().trim());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (embeddingChanged) {
            validateEmbeddingConnection(entity);
        }
        EntityDefaults.update(entity);
        try {
            knowledgeBaseService.updateById(entity);
        } catch (DuplicateKeyException error) {
            throw new KnowledgeOperationException("同名知识库已存在", error);
        }
        return toKnowledgeBaseResponse(entity);
    }

    public boolean deleteKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBaseDeletion deletion = transactionTemplate.execute(status -> {
            AiKnowledgeBaseEntity knowledgeBase =
                    requireKnowledgeBaseForUpdate(knowledgeBaseId);
            if (!Objects.equals(knowledgeBase.getStatus(), ENABLED)) {
                throw new KnowledgeOperationException(
                        "知识库已停用，不能删除，请先启用"
                );
            }
            assertNoActiveKnowledgeBaseProcessing(knowledgeBaseId);

            List<AiKnowledgeDocumentEntity> documents =
                    documentService.lambdaQuery()
                            .eq(
                                    AiKnowledgeDocumentEntity::getKnowledgeBaseId,
                                    knowledgeBase.getId()
                            )
                            .list();

            QueryWrapper<AiKnowledgeAgentBindingEntity> bindingQuery =
                    new QueryWrapper<>();
            bindingQuery.eq("knowledge_base_id", knowledgeBase.getId());
            bindingService.remove(bindingQuery);
            chunkMapper.physicalDeleteByKnowledgeBase(
                    knowledgeBase.getTenantId(),
                    knowledgeBase.getId()
            );

            for (AiKnowledgeDocumentEntity document : documents) {
                document.setParseStatus(DOCUMENT_DELETED).setStatus(DELETING);
                EntityDefaults.update(document);
                updateDocumentOrThrow(document);
                if (!documentService.removeById(document.getId())) {
                    throw new KnowledgeOperationException("知识文档删除失败");
                }
            }
            if (!knowledgeBaseService.removeById(knowledgeBase.getId())) {
                throw new KnowledgeOperationException("知识库删除失败");
            }
            return new KnowledgeBaseDeletion(knowledgeBase, documents);
        });
        if (deletion == null) {
            throw new KnowledgeOperationException("知识库数据库数据删除失败");
        }

        for (AiKnowledgeDocumentEntity document : deletion.documents()) {
            try {
                sourceStorage.delete(document.getSourceUri());
            } catch (Exception error) {
                log.error(
                        "Failed to delete knowledge source after database deletion, knowledgeBaseId={}, documentId={}",
                        deletion.knowledgeBase().getId(),
                        document.getId(),
                        error
                );
                throw new KnowledgeOperationException(
                        "删除知识源文件失败：" + document.getDocumentName(),
                        error
                );
            }
        }
        try {
            milvusStoreFactory.dropCollection(deletion.knowledgeBase());
        } catch (RuntimeException error) {
            log.error(
                    "Failed to delete vector collection after database deletion, knowledgeBaseId={}",
                    deletion.knowledgeBase().getId(),
                    error
            );
            throw new KnowledgeOperationException("删除向量库数据失败", error);
        }
        return true;
    }

    public IPage<KnowledgeDocumentResponse> pageDocuments(
            Long knowledgeBaseId,
            long current,
            long size,
            String keyword,
            String parseStatus
    ) {
        requireKnowledgeBase(knowledgeBaseId);
        QueryWrapper<AiKnowledgeDocumentEntity> query = new QueryWrapper<>();
        query.eq("knowledge_base_id", knowledgeBaseId);
        if (StringUtils.hasText(keyword)) {
            query.like("document_name", keyword.trim());
        }
        if (StringUtils.hasText(parseStatus)) {
            query.eq("parse_status", parseStatus.trim().toUpperCase(Locale.ROOT));
        }
        query.orderByDesc("updated_at").orderByDesc("created_at");
        return documentService
                .page(new Page<>(positive(current), pageSize(size)), query)
                .convert(this::toDocumentResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResponse uploadDocument(
            Long knowledgeBaseId,
            MultipartFile file
    ) {
        UserInfo user = currentUser();
        requireActiveKnowledgeBaseForUpdate(knowledgeBaseId);
        ValidatedUpload upload = uploadValidator.validate(file);
        assertDocumentNameAvailable(knowledgeBaseId, upload.originalName());

        Long documentId = IdWorker.getId();
        StoredSource stored = null;
        try {
            stored = sourceStorage.store(
                    user.getTenantId(),
                    knowledgeBaseId,
                    documentId,
                    1,
                    upload.documentType().toLowerCase(Locale.ROOT),
                    file,
                    upload.maxBytes()
            );
            AiKnowledgeDocumentEntity entity = new AiKnowledgeDocumentEntity()
                    .setId(documentId)
                    .setKnowledgeBaseId(knowledgeBaseId)
                    .setExternalDocumentId(String.valueOf(documentId))
                    .setDocumentName(upload.originalName())
                    .setDocumentType(upload.documentType())
                    .setMimeType(upload.mimeType())
                    .setSourceType("UPLOAD")
                    .setSourceUri(stored.sourceUri())
                    .setSizeBytes(stored.sizeBytes())
                    .setChecksum(stored.checksum())
                    .setVersionNo(1)
                    .setParseStatus(DOCUMENT_UPLOADED)
                    .setChunkCount(0)
                    .setTokenCount(0)
                    .setStatus(ENABLED);
            entity.setTenantId(user.getTenantId());
            EntityDefaults.create(entity);
            documentService.save(entity);
            return toDocumentResponse(entity);
        } catch (Exception error) {
            if (stored != null) {
                try {
                    sourceStorage.delete(stored.sourceUri());
                } catch (Exception cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }
            if (error instanceof DuplicateKeyException) {
                throw new KnowledgeOperationException(
                        "该文档已存在，请先删除原有文档再上传",
                        error
                );
            }
            if (error instanceof KnowledgeOperationException operationError) {
                throw operationError;
            }
            throw new KnowledgeOperationException("保存上传文档失败", error);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResponse submitIndex(
            Long documentId,
            KnowledgeIndexRequest request
    ) {
        AiKnowledgeDocumentEntity snapshot = requireDocument(documentId);
        AiKnowledgeBaseEntity knowledgeBase =
                requireActiveKnowledgeBaseForUpdate(
                        snapshot.getKnowledgeBaseId()
                );
        AiKnowledgeDocumentEntity document =
                requireDocumentForUpdate(documentId);
        ensureDocumentBelongsToKnowledgeBase(document, knowledgeBase.getId());
        if (!DOCUMENT_UPLOADED.equals(document.getParseStatus())
                && !DOCUMENT_FAILED.equals(document.getParseStatus())) {
            throw new KnowledgeOperationException(
                    "只有已上传或切片入库失败的文档可以重新提交"
            );
        }
        ChunkConfig config = validateChunkConfig(request);

        document.setParseStatus(DOCUMENT_PENDING)
                .setErrorMessage(null)
                .setLeaseOwner(null)
                .setLeaseUntil(null);
        applyChunkConfig(document, config);
        EntityDefaults.update(document);
        updateDocumentOrThrow(document);
        documentDispatcher.dispatchAfterCommit(document.getId());
        return toDocumentResponse(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResponse deleteDocument(Long documentId) {
        AiKnowledgeDocumentEntity snapshot = requireDocument(documentId);
        AiKnowledgeBaseEntity knowledgeBase =
                requireActiveKnowledgeBaseForUpdate(
                        snapshot.getKnowledgeBaseId()
                );
        AiKnowledgeDocumentEntity document =
                requireDocumentForUpdate(documentId);
        ensureDocumentBelongsToKnowledgeBase(document, knowledgeBase.getId());
        boolean retryDelete = Objects.equals(document.getStatus(), DELETING)
                && DOCUMENT_DELETE_FAILED.equals(document.getParseStatus());
        if (Objects.equals(document.getStatus(), DELETING) && !retryDelete) {
            throw new KnowledgeOperationException(
                    "知识文档正在删除"
            );
        }
        if (!retryDelete && isActiveDocumentProcessing(document.getParseStatus())) {
            throw new KnowledgeOperationException(
                    "知识文档正在处理中，请等待完成后再删除"
            );
        }
        document.setParseStatus(DOCUMENT_DELETING)
                .setStatus(DELETING)
                .setErrorMessage(null)
                .setLeaseOwner(null)
                .setLeaseUntil(null);
        EntityDefaults.update(document);
        updateDocumentOrThrow(document);
        documentDispatcher.dispatchAfterCommit(document.getId());
        return toDocumentResponse(document);
    }

    public IPage<KnowledgeChunkResponse> pageChunks(
            Long documentId,
            long current,
            long size
    ) {
        requireDocument(documentId);
        QueryWrapper<AiKnowledgeChunkEntity> query = new QueryWrapper<>();
        query.eq("document_id", documentId)
                .orderByAsc("chunk_index");
        return chunkService
                .page(new Page<>(positive(current), pageSize(size)), query)
                .convert(this::toChunkResponse);
    }

    private AiKnowledgeBaseEntity requireKnowledgeBase(Long knowledgeBaseId) {
        currentUser();
        if (knowledgeBaseId == null) {
            throw new KnowledgeOperationException("知识库ID不能为空");
        }
        AiKnowledgeBaseEntity entity = knowledgeBaseService.getById(knowledgeBaseId);
        if (entity == null) {
            throw new KnowledgeOperationException("知识库不存在");
        }
        return entity;
    }

    private AiKnowledgeBaseEntity requireActiveKnowledgeBase(Long knowledgeBaseId) {
        AiKnowledgeBaseEntity entity = requireKnowledgeBase(knowledgeBaseId);
        if (!Objects.equals(entity.getStatus(), ENABLED)) {
            throw new KnowledgeOperationException(
                    Objects.equals(entity.getStatus(), DELETING)
                            ? "知识库删除中，不能执行该操作"
                            : "知识库已停用，不能执行该操作"
            );
        }
        return entity;
    }

    private AiKnowledgeBaseEntity requireKnowledgeBaseForUpdate(
            Long knowledgeBaseId
    ) {
        currentUser();
        if (knowledgeBaseId == null) {
            throw new KnowledgeOperationException("知识库ID不能为空");
        }
        AiKnowledgeBaseEntity entity =
                knowledgeBaseMapper.selectByIdForUpdate(knowledgeBaseId);
        if (entity == null) {
            throw new KnowledgeOperationException("知识库不存在");
        }
        return entity;
    }

    private AiKnowledgeBaseEntity requireActiveKnowledgeBaseForUpdate(
            Long knowledgeBaseId
    ) {
        AiKnowledgeBaseEntity entity =
                requireKnowledgeBaseForUpdate(knowledgeBaseId);
        if (!Objects.equals(entity.getStatus(), ENABLED)) {
            throw new KnowledgeOperationException(
                    Objects.equals(entity.getStatus(), DELETING)
                            ? "知识库删除中，不能执行该操作"
                            : "知识库已停用，不能执行该操作"
            );
        }
        return entity;
    }

    private AiKnowledgeDocumentEntity requireDocument(Long documentId) {
        currentUser();
        if (documentId == null) {
            throw new KnowledgeOperationException("知识文档ID不能为空");
        }
        AiKnowledgeDocumentEntity document = documentService.getById(documentId);
        if (document == null) {
            throw new KnowledgeOperationException("知识文档不存在");
        }
        return document;
    }

    private AiKnowledgeDocumentEntity requireDocumentForUpdate(
            Long documentId
    ) {
        currentUser();
        if (documentId == null) {
            throw new KnowledgeOperationException("知识文档ID不能为空");
        }
        AiKnowledgeDocumentEntity document =
                knowledgeDocumentMapper.selectByIdForUpdate(documentId);
        if (document == null) {
            throw new KnowledgeOperationException("知识文档不存在");
        }
        return document;
    }

    private static void ensureDocumentBelongsToKnowledgeBase(
            AiKnowledgeDocumentEntity document,
            Long knowledgeBaseId
    ) {
        if (!Objects.equals(document.getKnowledgeBaseId(), knowledgeBaseId)) {
            throw new KnowledgeOperationException("知识文档与知识库不匹配");
        }
    }

    private void updateDocumentOrThrow(
            AiKnowledgeDocumentEntity document
    ) {
        if (!documentService.updateById(document)) {
            throw new KnowledgeOperationException(
                    "知识文档状态已变化，请刷新后重试"
            );
        }
    }

    private void validateCreateRequest(KnowledgeBaseCreateRequest request) {
        if (request == null) {
            throw new KnowledgeOperationException("知识库创建参数不能为空");
        }
        normalizeRequired(request.getKnowledgeName(), "知识库名称");
        normalizeRequired(request.getModelUrl(), "Embedding URL");
        normalizeRequired(request.getApiKey(), "API Key");
        normalizeRequired(request.getEmbeddingModelName(), "Embedding 模型名");
        if (request.getEmbeddingDimension() == null
                || request.getEmbeddingDimension() <= 0) {
            throw new KnowledgeOperationException("Embedding 维度必须为正整数");
        }
        normalizeMetric(request.getMetricType());
        defaultTopK(request.getTopK());
        defaultThreshold(request.getScoreThreshold());
    }

    private void validateEmbeddingConnection(AiKnowledgeBaseEntity entity) {
        EmbeddingModel embeddingModel = null;
        try {
            embeddingModel = embeddingModelFactory.create(entity);
            double[] vector = embeddingModel
                    .embed(TextBlock.builder()
                            .text("knowledge base connection test")
                            .build())
                    .block(Duration.ofSeconds(60));
            if (vector == null || vector.length != entity.getEmbeddingDimension()) {
                throw new KnowledgeOperationException(
                        "Embedding 返回维度与配置不一致"
                );
            }
        } catch (KnowledgeOperationException error) {
            throw error;
        } catch (Exception error) {
            throw new KnowledgeOperationException(
                    "Embedding 配置验证失败："
                            + safeMessage(error, entity.getApiKey()),
                    error
            );
        } finally {
            closeIfNeeded(embeddingModel);
        }
    }

    private void assertKnowledgeBaseNameAvailable(String name, Long excludedId) {
        QueryWrapper<AiKnowledgeBaseEntity> query = new QueryWrapper<>();
        query.eq("active_knowledge_name", normalizeNameForUniqueness(name));
        if (excludedId != null) {
            query.ne("id", excludedId);
        }
        if (knowledgeBaseService.count(query) > 0) {
            throw new KnowledgeOperationException("同名知识库已存在");
        }
    }

    private void assertDocumentNameAvailable(
            Long knowledgeBaseId,
            String documentName
    ) {
        QueryWrapper<AiKnowledgeDocumentEntity> query = new QueryWrapper<>();
        query.eq("knowledge_base_id", knowledgeBaseId)
                .eq(
                        "active_document_name",
                        normalizeNameForUniqueness(documentName)
                );
        if (documentService.count(query) > 0) {
            throw new KnowledgeOperationException(
                    "该文档已存在，请先删除原有文档再上传"
            );
        }
    }

    private void assertNoActiveKnowledgeBaseProcessing(Long knowledgeBaseId) {
        QueryWrapper<AiKnowledgeDocumentEntity> query = new QueryWrapper<>();
        query.eq("knowledge_base_id", knowledgeBaseId)
                .in(
                        "parse_status",
                        List.of(
                                DOCUMENT_PENDING,
                                DOCUMENT_PARSING,
                                DOCUMENT_CHUNKING,
                                DOCUMENT_EMBEDDING,
                                DOCUMENT_INDEXING,
                                DOCUMENT_DELETING
                        )
                );
        if (documentService.count(query) > 0) {
            throw new KnowledgeOperationException(
                    "知识库仍有处理中的知识文档，请等待完成后再删除"
            );
        }
    }

    private static boolean isActiveDocumentProcessing(String parseStatus) {
        return List.of(
                DOCUMENT_PENDING,
                DOCUMENT_PARSING,
                DOCUMENT_CHUNKING,
                DOCUMENT_EMBEDDING,
                DOCUMENT_INDEXING,
                DOCUMENT_DELETING
        ).contains(parseStatus);
    }

    private ChunkConfig validateChunkConfig(KnowledgeIndexRequest request) {
        if (request == null || !StringUtils.hasText(request.getChunkStrategy())) {
            throw new KnowledgeOperationException("请选择切片策略");
        }
        String strategy = request.getChunkStrategy()
                .trim()
                .toUpperCase(Locale.ROOT);
        if (CHUNK_DELIMITER.equals(strategy)) {
            String delimiter = unescapeDelimiter(request.getChunkDelimiter());
            if (delimiter.length() > MAX_DELIMITER_LENGTH) {
                throw new KnowledgeOperationException("分隔符长度必须为 1–32 个字符");
            }
            return new ChunkConfig(strategy, null, null, delimiter);
        }
        if (!CHUNK_CHARACTER.equals(strategy) && !CHUNK_PARAGRAPH.equals(strategy)) {
            throw new KnowledgeOperationException("不支持的切片策略");
        }
        int size = request.getChunkSize() == null
                ? DEFAULT_CHUNK_SIZE
                : request.getChunkSize();
        int overlap = request.getChunkOverlap() == null
                ? DEFAULT_CHUNK_OVERLAP
                : request.getChunkOverlap();
        if (size < MIN_CHUNK_SIZE || size > MAX_CHUNK_SIZE) {
            throw new KnowledgeOperationException("切片大小必须在 200–4000 之间");
        }
        if (overlap < 0
                || overlap > MAX_CHUNK_OVERLAP
                || overlap >= size) {
            throw new KnowledgeOperationException(
                    "重叠大小必须在 0–500 之间且小于切片大小"
            );
        }
        return new ChunkConfig(strategy, size, overlap, null);
    }

    static String unescapeDelimiter(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new KnowledgeOperationException("指定字符切片必须填写分隔符");
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current != '\\') {
                result.append(current);
                continue;
            }
            if (++i >= raw.length()) {
                throw new KnowledgeOperationException("分隔符包含未完成的转义");
            }
            char escaped = raw.charAt(i);
            result.append(switch (escaped) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case '\\' -> '\\';
                default -> throw new KnowledgeOperationException(
                        "分隔符仅支持 \\\\n、\\\\r、\\\\t、\\\\\\\\ 转义"
                );
            });
        }
        if (result.isEmpty()) {
            throw new KnowledgeOperationException("分隔符不能为空");
        }
        return result.toString();
    }

    private static void applyChunkConfig(
            AiKnowledgeDocumentEntity document,
            ChunkConfig config
    ) {
        document.setChunkStrategy(config.strategy())
                .setChunkSize(config.chunkSize())
                .setChunkOverlap(config.chunkOverlap())
                .setChunkDelimiter(config.delimiter());
    }

    private KnowledgeBaseResponse toKnowledgeBaseResponse(
            AiKnowledgeBaseEntity entity
    ) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        response.setId(entity.getId());
        response.setKnowledgeName(entity.getKnowledgeName());
        response.setDescription(entity.getDescription());
        response.setModelUrl(entity.getModelUrl());
        response.setEmbeddingModelName(entity.getEmbeddingModelName());
        response.setEmbeddingDimension(entity.getEmbeddingDimension());
        response.setMetricType(entity.getMetricType());
        response.setTopK(entity.getTopK());
        response.setScoreThreshold(entity.getScoreThreshold());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setDocumentCount(documentService.lambdaQuery()
                .eq(
                        AiKnowledgeDocumentEntity::getKnowledgeBaseId,
                        entity.getId()
                )
                .count());
        return response;
    }

    public KnowledgeDocumentResponse toDocumentResponse(
            AiKnowledgeDocumentEntity entity
    ) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setId(entity.getId());
        response.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        response.setDocumentName(entity.getDocumentName());
        response.setDocumentType(entity.getDocumentType());
        response.setMimeType(entity.getMimeType());
        response.setSizeBytes(entity.getSizeBytes());
        response.setChecksum(entity.getChecksum());
        response.setParseStatus(entity.getParseStatus());
        response.setChunkStrategy(entity.getChunkStrategy());
        response.setChunkSize(entity.getChunkSize());
        response.setChunkOverlap(entity.getChunkOverlap());
        response.setChunkDelimiter(entity.getChunkDelimiter());
        response.setChunkCount(entity.getChunkCount());
        response.setTokenCount(entity.getTokenCount());
        response.setErrorMessage(entity.getErrorMessage());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public KnowledgeChunkResponse toChunkResponse(AiKnowledgeChunkEntity entity) {
        KnowledgeChunkResponse response = new KnowledgeChunkResponse();
        response.setId(entity.getId());
        response.setDocumentId(entity.getDocumentId());
        response.setChunkIndex(entity.getChunkIndex());
        response.setChunkUid(entity.getChunkUid());
        response.setContent(entity.getContent());
        response.setContentHash(entity.getContentHash());
        response.setContentType(entity.getContentType());
        response.setPageNo(entity.getPageNo());
        response.setSectionTitle(entity.getSectionTitle());
        response.setStartOffset(entity.getStartOffset());
        response.setEndOffset(entity.getEndOffset());
        response.setTokenCount(entity.getTokenCount());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private static UserInfo currentUser() {
        UserInfo user = UserContext.get();
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new KnowledgeOperationException("用户未登录或租户信息缺失");
        }
        return user;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new KnowledgeOperationException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeNameForUniqueness(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMetric(String value) {
        String metric = StringUtils.hasText(value)
                ? value.trim().toUpperCase(Locale.ROOT)
                : "COSINE";
        if (!SUPPORTED_METRICS.contains(metric)) {
            throw new KnowledgeOperationException(
                    "距离度量只支持 COSINE、IP、L2"
            );
        }
        return metric;
    }

    private static int defaultTopK(Integer value) {
        int topK = value == null ? 5 : value;
        if (topK < 1 || topK > 20) {
            throw new KnowledgeOperationException("TopK 必须在 1–20 之间");
        }
        return topK;
    }

    private static BigDecimal defaultThreshold(BigDecimal value) {
        BigDecimal threshold = value == null
                ? new BigDecimal("0.5")
                : value;
        if (threshold.compareTo(BigDecimal.ZERO) < 0
                || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new KnowledgeOperationException("相似度阈值必须在 0–1 之间");
        }
        return threshold;
    }

    private static long positive(long value) {
        return value <= 0 ? 1 : value;
    }

    private static long pageSize(long value) {
        if (value <= 0) {
            return 10;
        }
        return Math.min(value, MAX_PAGE_SIZE);
    }

    private static String safeMessage(Throwable error, String secret) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = StringUtils.hasText(cursor.getMessage())
                ? cursor.getMessage()
                : "外部服务不可用";
        if (StringUtils.hasText(secret)) {
            message = message.replace(secret, "***");
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private static void closeIfNeeded(Object resource) {
        if (resource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 验证调用已完成，关闭异常不覆盖原验证结果。
            }
        }
    }

    private record KnowledgeBaseDeletion(
            AiKnowledgeBaseEntity knowledgeBase,
            List<AiKnowledgeDocumentEntity> documents
    ) {
    }

    private record ChunkConfig(
            String strategy,
            Integer chunkSize,
            Integer chunkOverlap,
            String delimiter
    ) {
    }
}
