package com.zw.agent.knowledge;

/**
 * 可安全直接展示给知识管理页面的业务错误。
 */
public class KnowledgeOperationException extends RuntimeException {

    public KnowledgeOperationException(String message) {
        super(message);
    }

    public KnowledgeOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
