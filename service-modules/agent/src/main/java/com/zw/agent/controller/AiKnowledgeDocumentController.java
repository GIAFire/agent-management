package com.zw.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.service.AiKnowledgeDocumentService;
import com.zw.common.entity.Result;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai-knowledge-document-entity")
@RequiredArgsConstructor
public class AiKnowledgeDocumentController {

    private static final String UPLOAD_DIR = "knowledge-uploads";

    private final AiKnowledgeDocumentService aiKnowledgeDocumentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AiKnowledgeDocumentEntity> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam(value = "workspaceFileId", required = false) Long workspaceFileId,
            @RequestParam(value = "language", required = false) String language
    ) {
        if (file == null || file.isEmpty()) {
            return Result.fail("Upload file must not be empty");
        }
        if (knowledgeBaseId == null) {
            return Result.fail("knowledgeBaseId must not be null");
        }

        try {
            Path uploadDir = projectRoot().resolve(UPLOAD_DIR);
            Files.createDirectories(uploadDir);

            String originalFilename = safeOriginalFilename(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + "-" + originalFilename;
            Path target = uploadDir.resolve(storedFilename).normalize();
            if (!target.startsWith(uploadDir)) {
                return Result.fail("Invalid file name");
            }

            file.transferTo(target.toFile());

            AiKnowledgeDocumentEntity entity = new AiKnowledgeDocumentEntity()
                    .setKnowledgeBaseId(knowledgeBaseId)
                    .setWorkspaceFileId(workspaceFileId)
                    .setExternalDocumentId(storedFilename)
                    .setDocumentName(originalFilename)
                    .setDocumentType(documentType(originalFilename))
                    .setMimeType(resolveMimeType(file, target))
                    .setSourceType("UPLOAD")
                    .setSourceUri(target.toAbsolutePath().toString())
                    .setSizeBytes(Files.size(target))
                    .setChecksum(sha256(target))
                    .setLanguage(language)
                    .setVersionNo(1)
                    .setParseStatus("UPLOADED")
                    .setChunkCount(0)
                    .setTokenCount(0)
                    .setStatus((byte) 1);

            aiKnowledgeDocumentService.save(EntityDefaults.create(entity));
            return Result.ok(entity);
        } catch (Exception e) {
            return Result.fail("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<AiKnowledgeDocumentEntity>> list() {
        return Result.ok(aiKnowledgeDocumentService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AiKnowledgeDocumentEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(aiKnowledgeDocumentService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeDocumentEntity> getById(@PathVariable Long id) {
        return Result.ok(aiKnowledgeDocumentService.getById(id));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AiKnowledgeDocumentEntity entity) {
        return Result.ok(aiKnowledgeDocumentService.save(EntityDefaults.create(entity)));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AiKnowledgeDocumentEntity entity) {
        return Result.ok(aiKnowledgeDocumentService.updateById(EntityDefaults.update(entity)));
    }

    @GetMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(aiKnowledgeDocumentService.removeById(id));
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        Path cursor = current;
        while (cursor != null) {
            if (Files.exists(cursor.resolve("pom.xml")) && Files.isDirectory(cursor.resolve("service-modules"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    private String safeOriginalFilename(String originalFilename) {
        String filename = originalFilename == null || originalFilename.isBlank()
                ? "document.txt"
                : originalFilename.replace("\\", "/");
        int slashIndex = filename.lastIndexOf('/');
        if (slashIndex >= 0) {
            filename = filename.substring(slashIndex + 1);
        }
        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return filename.isBlank() ? "document.txt" : filename;
    }

    private String documentType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "UNKNOWN";
        }
        return filename.substring(dotIndex + 1).toUpperCase();
    }

    private String resolveMimeType(MultipartFile file, Path target) throws IOException {
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType();
        }
        String probedType = Files.probeContentType(target);
        return probedType == null ? "application/octet-stream" : probedType;
    }

    private String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
