package com.zw.agent.knowledge.storage;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识源文件存储边界，业务层只保存实现无关的相对 URI。
 */
public interface KnowledgeSourceStorage {

    StoredSource store(
            Long tenantId,
            Long knowledgeBaseId,
            Long documentId,
            int version,
            String extension,
            MultipartFile file,
            long maxBytes
    ) throws IOException;

    Path resolve(String sourceUri);

    void delete(String sourceUri) throws IOException;

    record StoredSource(String sourceUri, long sizeBytes, String checksum) {
    }
}
