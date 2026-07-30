package com.zw.agent.knowledge.processing;

import com.zw.agent.knowledge.KnowledgeOperationException;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.PDFReader;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TikaReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentParser {

    public String extractText(Path sourcePath, String documentType) {
        try {
            List<Document> parsed;
            if ("TXT".equalsIgnoreCase(documentType)
                    || "MD".equalsIgnoreCase(documentType)) {
                String text = Files.readString(sourcePath, StandardCharsets.UTF_8);
                return requireText(text);
            }
            if ("PDF".equalsIgnoreCase(documentType)) {
                // PDFReader 仅提取文本层，不触发 OCR。
                parsed = new PDFReader(
                        Integer.MAX_VALUE,
                        SplitStrategy.CHARACTER,
                        0
                ).read(ReaderInput.fromPath(sourcePath)).block();
            } else {
                TikaReader reader = new TikaReader(
                        Integer.MAX_VALUE,
                        SplitStrategy.CHARACTER,
                        0,
                        new BodyContentHandler(-1)
                );
                parsed = reader.read(ReaderInput.fromPath(sourcePath)).block();
            }
            if (parsed == null || parsed.isEmpty()) {
                throw new KnowledgeOperationException(
                        "文档未提取到可索引文本，可能是扫描件、加密文档或空文档"
                );
            }
            String text = parsed.stream()
                    .filter(Objects::nonNull)
                    .map(Document::getMetadata)
                    .filter(Objects::nonNull)
                    .map(metadata -> metadata.getContentText())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            return requireText(text);
        } catch (KnowledgeOperationException error) {
            throw error;
        } catch (Exception error) {
            throw new KnowledgeOperationException(
                    "文档解析失败，可能是文件损坏、加密或格式不受支持",
                    error
            );
        }
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new KnowledgeOperationException(
                    "文档未提取到可索引文本，可能是扫描件、加密文档或空文档"
            );
        }
        return text;
    }
}
