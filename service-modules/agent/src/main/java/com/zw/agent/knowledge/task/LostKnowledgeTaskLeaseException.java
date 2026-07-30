package com.zw.agent.knowledge.task;

final class LostKnowledgeTaskLeaseException extends RuntimeException {

    LostKnowledgeTaskLeaseException(Long taskId) {
        super("Knowledge task lease lost: " + taskId);
    }
}
