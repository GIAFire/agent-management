package com.zw.agent.knowledge.task;

import com.zw.agent.config.knowledge.KnowledgeProperties;
import com.zw.agent.entity.AiKnowledgeTaskEntity;
import com.zw.agent.mapper.AiKnowledgeTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeTaskWorker {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeTaskWorker.class);

    private final AiKnowledgeTaskMapper taskMapper;
    private final KnowledgeTaskExecutionService executionService;
    private final KnowledgeProperties properties;
    private final Executor executor;
    private final Semaphore executionPermits;
    private final String workerInstanceId =
            UUID.randomUUID().toString().replace("-", "");

    public KnowledgeTaskWorker(
            AiKnowledgeTaskMapper taskMapper,
            KnowledgeTaskExecutionService executionService,
            KnowledgeProperties properties,
            @Qualifier("knowledgeTaskExecutor") Executor executor
    ) {
        this.taskMapper = taskMapper;
        this.executionService = executionService;
        this.properties = properties;
        this.executor = executor;
        this.executionPermits = new Semaphore(
                Math.max(1, properties.getTasks().getConcurrency())
        );
    }

//    @Scheduled(fixedDelayString = "${knowledge.tasks.poll-interval:2s}")
    public void poll() {
        cleanExpiredTasks();
        int availableSlots = executionPermits.availablePermits();
        if (availableSlots == 0) {
            return;
        }
        int batchSize = Math.min(
                availableSlots,
                Math.max(1, properties.getTasks().getClaimBatchSize())
        );
        List<Long> taskIds = taskMapper.findPendingTaskIds(batchSize);
        for (Long taskId : taskIds) {
            if (!executionPermits.tryAcquire()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            String leaseOwner = newLeaseOwner("execute");
            int claimed = taskMapper.claim(
                    taskId,
                    leaseOwner,
                    now.plus(properties.getTasks().getLeaseDuration()),
                    now
            );
            if (claimed != 1) {
                executionPermits.release();
                continue;
            }
            try {
                executor.execute(
                        () -> {
                            try {
                                executionService.execute(taskId, leaseOwner);
                            } finally {
                                executionPermits.release();
                            }
                        }
                );
            } catch (RuntimeException rejected) {
                try {
                    AiKnowledgeTaskEntity task =
                            taskMapper.selectWorkerTask(taskId);
                    if (task != null) {
                        executionService.rejectBeforeExecution(
                                task,
                                leaseOwner
                        );
                    }
                } finally {
                    executionPermits.release();
                }
                log.warn("Knowledge task rejected by bounded executor, taskId={}", taskId);
            }
        }
    }

    private void cleanExpiredTasks() {
        int availableSlots = executionPermits.availablePermits();
        if (availableSlots == 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int batchSize = Math.min(
                availableSlots,
                Math.max(1, properties.getTasks().getClaimBatchSize())
        );
        List<AiKnowledgeTaskEntity> expired =
                taskMapper.findExpiredRunningTasks(now, batchSize);
        for (AiKnowledgeTaskEntity task : expired) {
            if (!executionPermits.tryAcquire()) {
                return;
            }
            try {
                executor.execute(() -> {
                    try {
                        LocalDateTime reclaimTime = LocalDateTime.now();
                        String recoveryOwner = newLeaseOwner("recover");
                        int reclaimed = taskMapper.reclaimExpired(
                                task.getId(),
                                recoveryOwner,
                                reclaimTime.plus(
                                        properties.getTasks()
                                                .getLeaseDuration()
                                ),
                                reclaimTime
                        );
                        if (reclaimed == 1) {
                            executionService.cleanExpiredTask(
                                    task,
                                    recoveryOwner
                            );
                        }
                    } finally {
                        executionPermits.release();
                    }
                });
            } catch (RuntimeException rejected) {
                executionPermits.release();
                log.warn(
                        "Expired knowledge task cleanup rejected, taskId={}",
                        task.getId()
                );
            }
        }
    }

    private String newLeaseOwner(String phase) {
        return workerInstanceId
                + ":"
                + phase
                + ":"
                + UUID.randomUUID().toString().replace("-", "");
    }
}
