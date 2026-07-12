package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeBackendConfigEntity;
import com.zw.agent.entity.AiKnowledgeBaseEntity;
import com.zw.agent.entity.AiKnowledgeChunkEntity;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.factory.RAGFactory.RagBackendFactory;
import com.zw.agent.factory.RAGFactory.backendImpl.KnowledgeBackend;
import com.zw.agent.factory.RAGFactory.entity.ChunkResult;
import com.zw.agent.factory.RAGFactory.entity.KnowledgeBase;
import com.zw.agent.service.AiKnowledgeBackendConfigService;
import com.zw.agent.service.AiKnowledgeBaseService;
import com.zw.agent.service.AiKnowledgeChunkService;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/chunk")
@RequiredArgsConstructor
public class AiKnowledgeChunkController {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    private final AiKnowledgeChunkService aiKnowledgeChunkService;
    private final AiKnowledgeDocumentService aiKnowledgeDocumentService;
    private final AiKnowledgeBaseService aiKnowledgeBaseService;
    private final AiKnowledgeBackendConfigService aiKnowledgeBackendConfigService;
    private final RagBackendFactory ragBackendFactory;

    @GetMapping("/document/{documentId}")
    public Result<List<AiKnowledgeChunkEntity>> listByDocument(@PathVariable Long documentId) {
        return Result.ok(aiKnowledgeChunkService.list(
                new LambdaQueryWrapper<AiKnowledgeChunkEntity>()
                        .eq(AiKnowledgeChunkEntity::getDocumentId, documentId)
                        .orderByAsc(AiKnowledgeChunkEntity::getChunkIndex)
        ));
    }

    @PostMapping("/index")
    public Result<ChunkIndexResponse> index(@RequestBody ChunkIndexRequest request) {
        if (request == null || request.getDocumentId() == null) {
            return Result.fail("documentId must not be null");
        }

        AiKnowledgeDocumentEntity document = aiKnowledgeDocumentService.getById(request.getDocumentId());
        if (document == null) {
            return Result.fail("Document not found");
        }

        try {
            updateDocument(document, "CHUNKING", null, null, null);

            AiKnowledgeBaseEntity knowledgeBaseEntity = document.getKnowledgeBaseId() == null
                    ? null
                    : aiKnowledgeBaseService.getById(document.getKnowledgeBaseId());
            Long backendConfigId = resolveBackendConfigId(request, knowledgeBaseEntity);
            if (backendConfigId == null) {
                throw new IllegalArgumentException("backendConfigId must not be null");
            }

            AiKnowledgeBackendConfigEntity backendConfig = aiKnowledgeBackendConfigService.getById(backendConfigId);
            if (backendConfig == null) {
                throw new IllegalArgumentException("Knowledge backend config not found");
            }

            String content = readDocumentContent(document);
            if (content.isBlank()) {
                throw new IllegalArgumentException("Document content is empty");
            }

            int chunkSize = firstPositive(request.getChunkSize(), knowledgeBaseEntity == null ? null : knowledgeBaseEntity.getChunkSize(), DEFAULT_CHUNK_SIZE);
            int chunkOverlap = firstNonNegative(request.getChunkOverlap(), knowledgeBaseEntity == null ? null : knowledgeBaseEntity.getChunkOverlap(), DEFAULT_CHUNK_OVERLAP);
            if (chunkOverlap >= chunkSize) {
                throw new IllegalArgumentException("chunkOverlap must be smaller than chunkSize");
            }

            boolean overwrite = request.getOverwrite() == null || request.getOverwrite();
            LambdaQueryWrapper<AiKnowledgeChunkEntity> documentChunkQuery = new LambdaQueryWrapper<AiKnowledgeChunkEntity>()
                    .eq(AiKnowledgeChunkEntity::getDocumentId, document.getId());
            if (!overwrite && aiKnowledgeChunkService.count(documentChunkQuery) > 0) {
                throw new IllegalArgumentException("Document chunks already exist");
            }
            if (overwrite) {
                aiKnowledgeChunkService.remove(documentChunkQuery);
            }

            List<ChunkSlice> slices = splitContent(content, chunkSize, chunkOverlap);
            List<AiKnowledgeChunkEntity> chunkEntities = buildChunkEntities(document, backendConfig, slices);
            List<TextSegment> textSegments = buildTextSegments(document, chunkEntities);
            KnowledgeBase knowledgeBase = buildKnowledgeBase(document, knowledgeBaseEntity, backendConfigId);
            KnowledgeBackend backend = ragBackendFactory.create(backendConfig, knowledgeBase);

            List<ChunkResult> chunkResults = backend.indexChunks(knowledgeBase, textSegments);
            applyVectorResults(chunkEntities, chunkResults);

            boolean saved = aiKnowledgeChunkService.saveBatch(chunkEntities);
            if (!saved) {
                throw new IllegalStateException("Save chunks failed");
            }

            int tokenCount = chunkEntities.stream()
                    .map(AiKnowledgeChunkEntity::getTokenCount)
                    .mapToInt(value -> value == null ? 0 : value)
                    .sum();
            updateDocument(document, "READY", chunkEntities.size(), tokenCount, null);

            ChunkIndexResponse response = new ChunkIndexResponse();
            response.setDocumentId(document.getId());
            response.setKnowledgeBaseId(document.getKnowledgeBaseId());
            response.setBackendConfigId(backendConfigId);
            response.setChunkCount(chunkEntities.size());
            response.setTokenCount(tokenCount);
            return Result.ok(response);
        } catch (Exception e) {
            updateDocument(document, "FAILED", null, null, e.getMessage());
            return Result.fail("Chunk index failed: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<AiKnowledgeChunkEntity>> list() {
        return Result.ok(aiKnowledgeChunkService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeChunkEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeChunkService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeChunkEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeChunkService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeChunkEntity entity) {
        return Result.ok(aiKnowledgeChunkService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeChunkEntity entity) {
        return Result.ok(aiKnowledgeChunkService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeChunkService.removeById(id));
    }

    private Long resolveBackendConfigId(ChunkIndexRequest request, AiKnowledgeBaseEntity knowledgeBaseEntity) {
        if (request.getBackendConfigId() != null) {
            return request.getBackendConfigId();
        }
        return knowledgeBaseEntity == null ? null : knowledgeBaseEntity.getKnowledgeBackendId();
    }

    private String readDocumentContent(AiKnowledgeDocumentEntity document) throws IOException {
        if (document.getSourceUri() == null || document.getSourceUri().isBlank()) {
            throw new IllegalArgumentException("Document sourceUri is empty");
        }
        if (!isTextReadable(document)) {
            throw new IllegalArgumentException("Only text-like documents are supported for now");
        }

        Path path = Path.of(document.getSourceUri());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Document file does not exist: " + document.getSourceUri());
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private boolean isTextReadable(AiKnowledgeDocumentEntity document) {
        String documentType = normalize(document.getDocumentType());
        String mimeType = document.getMimeType() == null ? "" : document.getMimeType().toLowerCase(Locale.ROOT);
        if (mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml")
                || mimeType.contains("csv") || mimeType.contains("markdown")) {
            return true;
        }
        return List.of("TXT", "MD", "MARKDOWN", "CSV", "JSON", "HTML", "HTM", "XML", "YAML", "YML", "LOG", "SQL")
                .contains(documentType);
    }

    private List<ChunkSlice> splitContent(String content, int chunkSize, int chunkOverlap) {
        List<ChunkSlice> slices = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            if (end < content.length()) {
                end = preferredBreak(content, start, end, chunkSize);
            }

            int contentStart = trimStart(content, start, end);
            int contentEnd = trimEnd(content, contentStart, end);
            if (contentStart < contentEnd) {
                String chunkContent = content.substring(contentStart, contentEnd);
                slices.add(new ChunkSlice(chunkContent, contentStart, contentEnd, sectionTitle(chunkContent)));
            }

            if (end >= content.length()) {
                break;
            }
            int nextStart = Math.max(end - chunkOverlap, start + 1);
            start = trimStart(content, nextStart, content.length());
        }
        return slices;
    }

    private int preferredBreak(String content, int start, int end, int chunkSize) {
        int minBreak = start + Math.max(chunkSize / 2, 1);
        String window = content.substring(start, end);
        for (String separator : List.of("\n\n", "\n", "。", ". ", "；", "; ", "，", ", ")) {
            int relative = window.lastIndexOf(separator);
            if (relative > 0) {
                int candidate = start + relative + separator.length();
                if (candidate >= minBreak) {
                    return candidate;
                }
            }
        }
        return end;
    }

    private List<AiKnowledgeChunkEntity> buildChunkEntities(
            AiKnowledgeDocumentEntity document,
            AiKnowledgeBackendConfigEntity backendConfig,
            List<ChunkSlice> slices
    ) {
        List<AiKnowledgeChunkEntity> chunks = new ArrayList<>(slices.size());
        for (int index = 0; index < slices.size(); index++) {
            ChunkSlice slice = slices.get(index);
            String contentHash = sha256(slice.content());
            String chunkUid = stableChunkUid(document.getId(), index, contentHash);

            AiKnowledgeChunkEntity chunk = new AiKnowledgeChunkEntity()
                    .setKnowledgeBaseId(document.getKnowledgeBaseId())
                    .setDocumentId(document.getId())
                    .setChunkIndex(index)
                    .setChunkUid(chunkUid)
                    .setExternalChunkId(chunkUid)
                    .setContent(slice.content())
                    .setContentHash(contentHash)
                    .setContentType("TEXT")
                    .setSectionTitle(slice.sectionTitle())
                    .setStartOffset(slice.startOffset())
                    .setEndOffset(slice.endOffset())
                    .setTokenCount(estimateTokenCount(slice.content()))
                    .setVectorStoreId(backendConfig.getId())
                    .setVectorId(chunkUid)
                    .setEmbeddingModelConfigId(backendConfig.getId())
                    .setEmbeddingDimension(backendConfig.getEmbeddingDimension())
                    .setMetadataJson(metadataJson(document, slice))
                    .setStatus((byte) 1);
            chunks.add(EntityDefaults.create(chunk));
        }
        return chunks;
    }

    private List<TextSegment> buildTextSegments(AiKnowledgeDocumentEntity document, List<AiKnowledgeChunkEntity> chunks) {
        return chunks.stream()
                .map(chunk -> TextSegment.from(chunk.getContent(), metadata(document, chunk)))
                .toList();
    }

    private Metadata metadata(AiKnowledgeDocumentEntity document, AiKnowledgeChunkEntity chunk) {
        Metadata metadata = new Metadata()
                .put("chunk_uid", chunk.getChunkUid())
                .put("chunk_index", chunk.getChunkIndex())
                .put("document_id", chunk.getDocumentId())
                .put("knowledge_base_id", chunk.getKnowledgeBaseId())
                .put("content_hash", chunk.getContentHash())
                .put("source_uri", document.getSourceUri());
        if (document.getDocumentName() != null) {
            metadata.put("document_name", document.getDocumentName());
        }
        if (chunk.getSectionTitle() != null) {
            metadata.put("section_title", chunk.getSectionTitle());
        }
        return metadata;
    }

    private KnowledgeBase buildKnowledgeBase(
            AiKnowledgeDocumentEntity document,
            AiKnowledgeBaseEntity knowledgeBaseEntity,
            Long backendConfigId
    ) {
        KnowledgeBase knowledgeBase = new KnowledgeBase()
                .setId(document.getKnowledgeBaseId())
                .setBackendConfigId(backendConfigId);
        if (knowledgeBaseEntity != null) {
            knowledgeBase
                    .setName(knowledgeBaseEntity.getKnowledgeName())
                    .setCollectionName(knowledgeBaseEntity.getCollectionName())
                    .setChunkStrategy(knowledgeBaseEntity.getChunkStrategy())
                    .setChunkSize(knowledgeBaseEntity.getChunkSize())
                    .setChunkOverlap(knowledgeBaseEntity.getChunkOverlap())
                    .setProviderMetaJson(knowledgeBaseEntity.getProviderMetaJson());
        }
        return knowledgeBase;
    }

    private void applyVectorResults(List<AiKnowledgeChunkEntity> chunks, List<ChunkResult> results) {
        for (int index = 0; index < chunks.size(); index++) {
            String vectorId = index < results.size() && results.get(index).getVectorId() != null
                    ? results.get(index).getVectorId()
                    : chunks.get(index).getChunkUid();
            chunks.get(index)
                    .setVectorId(vectorId)
                    .setExternalChunkId(vectorId);
        }
    }

    private void updateDocument(
            AiKnowledgeDocumentEntity document,
            String parseStatus,
            Integer chunkCount,
            Integer tokenCount,
            String errorMessage
    ) {
        document.setParseStatus(parseStatus);
        if (chunkCount != null) {
            document.setChunkCount(chunkCount);
        }
        if (tokenCount != null) {
            document.setTokenCount(tokenCount);
        }
        document.setErrorMessage(errorMessage);
        aiKnowledgeDocumentService.updateById(EntityDefaults.update(document));
    }

    private int firstPositive(Integer preferred, Integer fallback, int defaultValue) {
        if (preferred != null && preferred > 0) {
            return preferred;
        }
        if (fallback != null && fallback > 0) {
            return fallback;
        }
        return defaultValue;
    }

    private int firstNonNegative(Integer preferred, Integer fallback, int defaultValue) {
        if (preferred != null && preferred >= 0) {
            return preferred;
        }
        if (fallback != null && fallback >= 0) {
            return fallback;
        }
        return defaultValue;
    }

    private int trimStart(String content, int start, int end) {
        while (start < end && Character.isWhitespace(content.charAt(start))) {
            start++;
        }
        return start;
    }

    private int trimEnd(String content, int start, int end) {
        while (end > start && Character.isWhitespace(content.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    private String sectionTitle(String content) {
        String compact = content.strip();
        if (compact.isEmpty()) {
            return null;
        }
        int newlineIndex = compact.indexOf('\n');
        String title = newlineIndex >= 0 ? compact.substring(0, newlineIndex) : compact;
        return title.length() > 120 ? title.substring(0, 120) : title;
    }

    private int estimateTokenCount(String content) {
        long cjkCount = content.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
                .count();
        long nonWhitespaceCount = content.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
        return Math.max(1, (int) Math.ceil(cjkCount + Math.max(0, nonWhitespaceCount - cjkCount) / 4.0));
    }

    private String stableChunkUid(Long documentId, int chunkIndex, String contentHash) {
        String value = "knowledge-chunk:" + documentId + ":" + chunkIndex + ":" + contentHash;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String metadataJson(AiKnowledgeDocumentEntity document, ChunkSlice slice) {
        return """
                {"documentName":"%s","sourceUri":"%s","startOffset":%d,"endOffset":%d}
                """.formatted(
                jsonEscape(document.getDocumentName()),
                jsonEscape(document.getSourceUri()),
                slice.startOffset(),
                slice.endOffset()
        ).trim();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ChunkSlice(String content, int startOffset, int endOffset, String sectionTitle) {
    }

    @Data
    public static class ChunkIndexRequest {
        private Long documentId;
        private Long backendConfigId;
        private Integer chunkSize;
        private Integer chunkOverlap;
        private Boolean overwrite;
    }

    @Data
    public static class ChunkIndexResponse {
        private Long documentId;
        private Long knowledgeBaseId;
        private Long backendConfigId;
        private Integer chunkCount;
        private Integer tokenCount;
    }
}
