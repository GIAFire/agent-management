package com.zw.agent.knowledge.processing;

import static com.zw.agent.knowledge.KnowledgeConstants.SUPPORTED_DOCUMENT_TYPES;

import com.zw.agent.config.knowledge.KnowledgeProperties;
import com.zw.agent.knowledge.KnowledgeOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class KnowledgeUploadValidator {

    private static final Map<String, Set<String>> ACCEPTED_MIME_TYPES = Map.of(
            "PDF", Set.of("application/pdf"),
            "DOC", Set.of(
                    "application/msword",
                    "application/x-tika-msoffice",
                    "application/vnd.ms-office"
            ),
            "DOCX", Set.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/x-tika-ooxml"
            ),
            "TXT", Set.of("text/plain"),
            "MD", Set.of(
                    "text/plain",
                    "text/markdown",
                    "text/x-markdown",
                    "text/x-web-markdown"
            )
    );

    private final KnowledgeProperties properties;

    private final Tika tika = new Tika();

    public KnowledgeUploadValidator(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KnowledgeOperationException("请选择要上传的文档");
        }
        long maxBytes = properties.getSource().getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new KnowledgeOperationException("文件大小不能超过 50 MB");
        }

        String originalName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName).toUpperCase(Locale.ROOT);
        if (!SUPPORTED_DOCUMENT_TYPES.contains(extension)) {
            throw new KnowledgeOperationException(
                    "仅支持 PDF、DOC、DOCX、TXT、MD 格式"
            );
        }

        String detectedMime;
        try (InputStream input = file.getInputStream()) {
            detectedMime = tika.detect(input, originalName);
        } catch (IOException error) {
            throw new KnowledgeOperationException("无法读取上传文件", error);
        }

        if (!ACCEPTED_MIME_TYPES.get(extension).contains(detectedMime)) {
            throw new KnowledgeOperationException(
                    "文件内容与扩展名不一致，检测到类型：" + detectedMime
            );
        }
        if ("TXT".equals(extension) || "MD".equals(extension)) {
            validateUtf8(file);
        }

        return new ValidatedUpload(
                originalName,
                extension,
                detectedMime,
                maxBytes
        );
    }

    private static void validateUtf8(MultipartFile file) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (InputStream input = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(input, decoder)) {
            char[] buffer = new char[4096];
            while (reader.read(buffer) >= 0) {
                // 只验证编码，不需要保留内容。
            }
        } catch (CharacterCodingException error) {
            throw new KnowledgeOperationException("TXT/MD 文档必须使用 UTF-8 编码");
        } catch (IOException error) {
            throw new KnowledgeOperationException("无法读取上传文件", error);
        }
    }

    private static String safeFileName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            throw new KnowledgeOperationException("文件名不能为空");
        }
        String normalized = originalName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(name) || ".".equals(name) || "..".equals(name)) {
            throw new KnowledgeOperationException("文件名不合法");
        }
        if (name.length() > 255 || name.chars().anyMatch(ch -> ch < 32)) {
            throw new KnowledgeOperationException("文件名不合法或过长");
        }
        return name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }

    public record ValidatedUpload(
            String originalName,
            String documentType,
            String mimeType,
            long maxBytes
    ) {
    }
}
