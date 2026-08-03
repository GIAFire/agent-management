package com.zhiran.agent.knowledge.processing;

final class LostKnowledgeDocumentLeaseException extends RuntimeException {

    LostKnowledgeDocumentLeaseException(Long documentId) {
        super("Knowledge document processing lease lost: " + documentId);
    }
}
