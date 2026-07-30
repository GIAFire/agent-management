package com.zw.agent.knowledge.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zw.agent.knowledge.KnowledgeOperationException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeChunkerTest {

    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test
    void splitsByLiteralDelimiterAndDiscardsEmptySegments() {
        List<KnowledgeChunker.ChunkPiece> chunks = chunker.split(
                " 第一段 \n\n\n\n 第二段\n\n ",
                "DELIMITER",
                null,
                null,
                "\n\n"
        );

        assertEquals(
                List.of("第一段", "第二段"),
                chunks.stream()
                        .map(KnowledgeChunker.ChunkPiece::content)
                        .toList()
        );
    }

    @Test
    void rejectsDelimiterSegmentOverFourThousandCharacters() {
        KnowledgeOperationException error = assertThrows(
                KnowledgeOperationException.class,
                () -> chunker.split(
                        "a".repeat(4001) + "\n\nok",
                        "DELIMITER",
                        null,
                        null,
                        "\n\n"
                )
        );

        assertTrue(error.getMessage().contains("第 1 个"));
        assertTrue(error.getMessage().contains("4001"));
    }

    @Test
    void normalizesWindowsLineEndingDelimiterWithDocumentText() {
        List<KnowledgeChunker.ChunkPiece> chunks = chunker.split(
                "第一段\r\n第二段",
                "DELIMITER",
                null,
                null,
                "\r\n"
        );

        assertEquals(List.of("第一段", "第二段"), chunks.stream()
                .map(KnowledgeChunker.ChunkPiece::content)
                .toList());
    }

    @Test
    void characterChunksUseConfiguredOverlap() {
        List<KnowledgeChunker.ChunkPiece> chunks = chunker.split(
                "abcdefghij",
                "CHARACTER",
                6,
                2,
                null
        );

        assertEquals(List.of("abcdef", "efghij"), chunks.stream()
                .map(KnowledgeChunker.ChunkPiece::content)
                .toList());
        assertEquals(4, chunks.get(1).startOffset());
    }

    @Test
    void emptyExtractedTextFailsInsteadOfCreatingEmptyVector() {
        assertThrows(
                KnowledgeOperationException.class,
                () -> chunker.split(
                        " \n\t ",
                        "PARAGRAPH",
                        1000,
                        100,
                        null
                )
        );
    }

    @Test
    void paragraphChunkingOfLongTextWithoutBlankLinesStaysLinear() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            List<KnowledgeChunker.ChunkPiece> chunks = chunker.split(
                    "a".repeat(2_000_000),
                    "PARAGRAPH",
                    200,
                    0,
                    null
            );
            assertEquals(10_000, chunks.size());
        });
    }
}
