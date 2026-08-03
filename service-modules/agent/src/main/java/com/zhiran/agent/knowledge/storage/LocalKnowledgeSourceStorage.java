package com.zhiran.agent.knowledge.storage;

import com.zhiran.agent.config.knowledge.KnowledgeProperties;
import com.zhiran.agent.knowledge.KnowledgeOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalKnowledgeSourceStorage implements KnowledgeSourceStorage {

    private final Path root;

    public LocalKnowledgeSourceStorage(KnowledgeProperties properties) {
        this.root = properties.getSource().getRoot().toAbsolutePath().normalize();
    }

    @Override
    public StoredSource store(
            Long tenantId,
            Long knowledgeBaseId,
            Long documentId,
            int version,
            String extension,
            MultipartFile file,
            long maxBytes
    ) throws IOException {
        String relativeUri = String.join(
                "/",
                String.valueOf(tenantId),
                String.valueOf(knowledgeBaseId),
                String.valueOf(documentId),
                "v" + version,
                UUID.randomUUID().toString().replace("-", "") + "." + extension.toLowerCase()
        );
        Path target = resolve(relativeUri);
        Files.createDirectories(target.getParent());

        MessageDigest digest = sha256();
        long written = 0L;
        try (InputStream input = file.getInputStream();
             OutputStream output = Files.newOutputStream(
                     target,
                     StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE
             )) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                written += read;
                if (written > maxBytes) {
                    throw new KnowledgeOperationException("文件大小不能超过 50 MB");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException error) {
            Files.deleteIfExists(target);
            throw error;
        }

        return new StoredSource(
                relativeUri,
                written,
                HexFormat.of().formatHex(digest.digest())
        );
    }

    @Override
    public Path resolve(String sourceUri) {
        if (sourceUri == null || sourceUri.isBlank()) {
            throw new KnowledgeOperationException("知识源文件地址为空");
        }
        Path resolved = root.resolve(sourceUri.replace('/', java.io.File.separatorChar))
                .toAbsolutePath()
                .normalize();
        if (!resolved.startsWith(root)) {
            throw new KnowledgeOperationException("知识源文件地址非法");
        }
        return resolved;
    }

    @Override
    public void delete(String sourceUri) throws IOException {
        if (sourceUri == null || sourceUri.isBlank()) {
            return;
        }
        Files.deleteIfExists(resolve(sourceUri));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM 不支持 SHA-256", impossible);
        }
    }
}
