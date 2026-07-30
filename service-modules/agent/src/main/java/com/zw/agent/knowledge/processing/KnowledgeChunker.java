package com.zw.agent.knowledge.processing;

import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_CHARACTER;
import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_DELIMITER;
import static com.zw.agent.knowledge.KnowledgeConstants.CHUNK_PARAGRAPH;
import static com.zw.agent.knowledge.KnowledgeConstants.MAX_CHUNK_SIZE;

import com.zw.agent.knowledge.KnowledgeOperationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeChunker {

    public List<ChunkPiece> split(
            String text,
            String strategy,
            Integer chunkSize,
            Integer overlap,
            String delimiter
    ) {
        if (text == null || text.isBlank()) {
            throw new KnowledgeOperationException(
                    "文档未提取到可索引文本，可能是扫描件、加密文档或空文档"
            );
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        return switch (strategy) {
            case CHUNK_CHARACTER ->
                    splitByWindow(normalized, chunkSize, overlap, false);
            case CHUNK_PARAGRAPH ->
                    splitByWindow(normalized, chunkSize, overlap, true);
            case CHUNK_DELIMITER ->
                    splitByDelimiter(
                            normalized,
                            delimiter == null
                                    ? null
                                    : delimiter.replace("\r\n", "\n")
                                            .replace('\r', '\n')
                    );
            default -> throw new KnowledgeOperationException("不支持的切片策略：" + strategy);
        };
    }

    private List<ChunkPiece> splitByWindow(
            String text,
            int size,
            int overlap,
            boolean preferParagraphBoundary
    ) {
        List<ChunkPiece> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            if (preferParagraphBoundary && end < text.length()) {
                int boundary = findParagraphBoundary(text, start, end, size);
                if (boundary > start) {
                    end = boundary;
                }
            }
            addTrimmed(chunks, text, start, end);
            if (end >= text.length()) {
                break;
            }
            int next = end - overlap;
            start = next > start ? next : end;
        }
        if (chunks.isEmpty()) {
            throw new KnowledgeOperationException("文档未提取到可索引文本");
        }
        return List.copyOf(chunks);
    }

    private static int findParagraphBoundary(
            String text,
            int start,
            int desiredEnd,
            int size
    ) {
        int minimumBoundary = start + Math.max(1, size / 2);
        for (int boundary = desiredEnd - 2;
                boundary >= minimumBoundary;
                boundary--) {
            if (text.charAt(boundary) == '\n'
                    && text.charAt(boundary + 1) == '\n') {
                return boundary + 2;
            }
        }
        return desiredEnd;
    }

    private List<ChunkPiece> splitByDelimiter(String text, String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            throw new KnowledgeOperationException("指定字符切片必须填写分隔符");
        }
        List<ChunkPiece> chunks = new ArrayList<>();
        int start = 0;
        int segmentIndex = 1;
        while (start <= text.length()) {
            int marker = text.indexOf(delimiter, start);
            int end = marker < 0 ? text.length() : marker;
            int before = chunks.size();
            addTrimmed(chunks, text, start, end);
            if (chunks.size() > before) {
                ChunkPiece piece = chunks.get(chunks.size() - 1);
                if (piece.content().length() > MAX_CHUNK_SIZE) {
                    throw new KnowledgeOperationException(
                            "第 " + segmentIndex + " 个分隔片段长度为 "
                                    + piece.content().length()
                                    + "，超过 4000 字符"
                    );
                }
            }
            if (marker < 0) {
                break;
            }
            start = marker + delimiter.length();
            segmentIndex++;
        }
        if (chunks.isEmpty()) {
            throw new KnowledgeOperationException("分隔后没有可索引的非空内容");
        }
        return List.copyOf(chunks);
    }

    private static void addTrimmed(
            List<ChunkPiece> chunks,
            String text,
            int rawStart,
            int rawEnd
    ) {
        int start = rawStart;
        int end = rawEnd;
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (start < end) {
            chunks.add(new ChunkPiece(text.substring(start, end), start, end));
        }
    }

    public record ChunkPiece(String content, int startOffset, int endOffset) {
    }
}
