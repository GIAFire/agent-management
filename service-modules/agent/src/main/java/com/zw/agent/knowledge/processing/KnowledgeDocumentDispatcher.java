package com.zw.agent.knowledge.processing;

import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_DELETING;
import static com.zw.agent.knowledge.KnowledgeConstants.DOCUMENT_PENDING;

import com.zw.agent.config.knowledge.KnowledgeProperties;
import com.zw.agent.entity.AiKnowledgeDocumentEntity;
import com.zw.agent.mapper.AiKnowledgeDocumentMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class KnowledgeDocumentDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeDocumentDispatcher.class);

    private final AiKnowledgeDocumentMapper documentMapper;
    private final KnowledgeDocumentProcessingService processingService;
    private final KnowledgeProperties properties;
    private final Executor dispatchExecutor;
    private final Executor processingExecutor;
    private final String dispatcherInstanceId =
            UUID.randomUUID().toString().replace("-", "");

    public KnowledgeDocumentDispatcher(
            AiKnowledgeDocumentMapper documentMapper,
            KnowledgeDocumentProcessingService processingService,
            KnowledgeProperties properties,
            @Qualifier("knowledgeDispatchExecutor") Executor dispatchExecutor,
            @Qualifier("knowledgeProcessingExecutor") Executor processingExecutor
    ) {
        this.documentMapper = documentMapper;
        this.processingService = processingService;
        this.properties = properties;
        this.dispatchExecutor = dispatchExecutor;
        this.processingExecutor = processingExecutor;
    }

    public void dispatchAfterCommit(Long documentId) {
        if (!properties.getProcessing().isEnabled()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueueDispatch(documentId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        enqueueDispatch(documentId);
                    }
                }
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnceOnStartup() {
        if (!properties.getProcessing().isEnabled()) {
            return;
        }
        try {
            dispatchExecutor.execute(this::safelyRecoverOnce);
        } catch (RuntimeException rejected) {
            log.error(
                    "Knowledge document startup recovery could not start",
                    rejected
            );
        }
    }

    private void enqueueDispatch(Long documentId) {
        try {
            dispatchExecutor.execute(() -> safelyDispatch(documentId));
        } catch (RuntimeException rejected) {
            log.error(
                    "Knowledge document could not be queued for dispatch, documentId={}",
                    documentId,
                    rejected
            );
        }
    }

    private void safelyDispatch(Long documentId) {
        try {
            dispatch(documentId);
        } catch (RuntimeException error) {
            log.error(
                    "Knowledge document dispatch failed, documentId={}",
                    documentId,
                    error
            );
        }
    }

    private void dispatch(Long documentId) {
        AiKnowledgeDocumentEntity document =
                documentMapper.selectWorkerDocument(documentId);
        if (document == null) {
            return;
        }
        String leaseOwner = newLeaseOwner("execute");
        if (claim(document, leaseOwner) != 1) {
            return;
        }
        try {
            processingExecutor.execute(
                    () -> processingService.execute(documentId, leaseOwner)
            );
        } catch (RuntimeException rejected) {
            AiKnowledgeDocumentEntity claimedDocument =
                    documentMapper.selectWorkerDocument(documentId);
            if (claimedDocument != null) {
                processingService.rejectBeforeExecution(
                        claimedDocument,
                        leaseOwner
                );
            }
            log.warn(
                    "Knowledge document processing rejected by bounded executor, documentId={}",
                    documentId
            );
        }
    }

    private void safelyRecoverOnce() {
        try {
            recoverExpiredDocuments();
            recoverPendingDocuments();
        } catch (RuntimeException error) {
            log.error("Knowledge document startup recovery failed", error);
        }
    }

    private void recoverExpiredDocuments() {
        int batchSize = recoveryBatchSize();
        while (true) {
            LocalDateTime now = LocalDateTime.now();
            List<AiKnowledgeDocumentEntity> expired =
                    documentMapper.findExpiredRecoveryCandidates(
                            now,
                            batchSize
                    );
            if (expired.isEmpty()) {
                return;
            }
            boolean recoveredAny = false;
            for (AiKnowledgeDocumentEntity document : expired) {
                String recoveryOwner = newLeaseOwner("recover");
                int reclaimed = documentMapper.reclaimExpired(
                        document.getId(),
                        recoveryOwner,
                        now.plus(
                                properties.getProcessing().getLeaseDuration()
                        ),
                        now
                );
                if (reclaimed == 1) {
                    recoveredAny = true;
                    document.setLeaseOwner(recoveryOwner);
                    processingService.cleanExpiredDocument(
                            document,
                            recoveryOwner
                    );
                }
            }
            if (!recoveredAny) {
                return;
            }
        }
    }

    private void recoverPendingDocuments() {
        int batchSize = recoveryBatchSize();
        while (true) {
            List<AiKnowledgeDocumentEntity> pending =
                    documentMapper.findPendingRecoveryCandidates(batchSize);
            if (pending.isEmpty()) {
                return;
            }
            boolean recoveredAny = false;
            for (AiKnowledgeDocumentEntity document : pending) {
                String leaseOwner = newLeaseOwner("startup");
                if (claim(document, leaseOwner) == 1) {
                    recoveredAny = true;
                    processingService.execute(document.getId(), leaseOwner);
                }
            }
            if (!recoveredAny) {
                return;
            }
        }
    }

    private int claim(
            AiKnowledgeDocumentEntity document,
            String leaseOwner
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseUntil = now.plus(
                properties.getProcessing().getLeaseDuration()
        );
        if (DOCUMENT_PENDING.equals(document.getParseStatus())) {
            return documentMapper.claimIndex(
                    document.getId(),
                    leaseOwner,
                    leaseUntil,
                    now
            );
        }
        if (DOCUMENT_DELETING.equals(document.getParseStatus())) {
            return documentMapper.claimDelete(
                    document.getId(),
                    leaseOwner,
                    leaseUntil,
                    now
            );
        }
        return 0;
    }

    private int recoveryBatchSize() {
        return Math.max(
                1,
                properties.getProcessing().getRecoveryBatchSize()
        );
    }

    private String newLeaseOwner(String phase) {
        return dispatcherInstanceId
                + ":"
                + phase
                + ":"
                + UUID.randomUUID().toString().replace("-", "");
    }
}
