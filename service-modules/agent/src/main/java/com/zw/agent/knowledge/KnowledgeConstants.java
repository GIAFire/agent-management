package com.zw.agent.knowledge;

import java.util.Set;

/**
 * 知识管理持久化状态值。保持为字符串/数字常量，避免数据库枚举演进成本。
 */
public final class KnowledgeConstants {

    public static final byte DISABLED = 0;
    public static final byte ENABLED = 1;
    public static final byte DELETING = 2;

    public static final String DOCUMENT_UPLOADED = "UPLOADED";
    public static final String DOCUMENT_PENDING = "PENDING";
    public static final String DOCUMENT_PARSING = "PARSING";
    public static final String DOCUMENT_CHUNKING = "CHUNKING";
    public static final String DOCUMENT_EMBEDDING = "EMBEDDING";
    public static final String DOCUMENT_INDEXING = "INDEXING";
    public static final String DOCUMENT_READY = "READY";
    public static final String DOCUMENT_FAILED = "FAILED";
    public static final String DOCUMENT_DELETING = "DELETING";
    public static final String DOCUMENT_DELETED = "DELETED";

    public static final String TASK_INDEX_DOCUMENT = "INDEX_DOCUMENT";
    public static final String TASK_DELETE_DOCUMENT = "DELETE_DOCUMENT";

    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_RUNNING = "RUNNING";
    public static final String TASK_SUCCEEDED = "SUCCEEDED";
    public static final String TASK_FAILED = "FAILED";

    public static final String CHUNK_CHARACTER = "CHARACTER";
    public static final String CHUNK_PARAGRAPH = "PARAGRAPH";
    public static final String CHUNK_DELIMITER = "DELIMITER";

    public static final int DEFAULT_CHUNK_SIZE = 1000;
    public static final int DEFAULT_CHUNK_OVERLAP = 100;
    public static final int MIN_CHUNK_SIZE = 200;
    public static final int MAX_CHUNK_SIZE = 4000;
    public static final int MAX_CHUNK_OVERLAP = 500;
    public static final int MAX_DELIMITER_LENGTH = 32;

    public static final Set<String> SUPPORTED_DOCUMENT_TYPES =
            Set.of("PDF", "DOC", "DOCX", "TXT", "MD");

    public static final Set<String> SUPPORTED_METRICS =
            Set.of("COSINE", "IP", "L2");

    private KnowledgeConstants() {
    }
}
