package com.zw.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.agent.entity.AiKnowledgeTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiKnowledgeTaskMapper extends BaseMapper<AiKnowledgeTaskEntity> {

    @InterceptorIgnore(tenantLine = "true")
    List<Long> findPendingTaskIds(@Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    int claim(
            @Param("taskId") Long taskId,
            @Param("workerId") String workerId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    AiKnowledgeTaskEntity selectWorkerTask(@Param("taskId") Long taskId);

    @InterceptorIgnore(tenantLine = "true")
    List<AiKnowledgeTaskEntity> findExpiredRunningTasks(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @InterceptorIgnore(tenantLine = "true")
    int reclaimExpired(
            @Param("taskId") Long taskId,
            @Param("workerId") String workerId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int heartbeat(
            @Param("taskId") Long taskId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("stage") String stage,
            @Param("progress") int progress,
            @Param("completedUnits") int completedUnits,
            @Param("totalUnits") int totalUnits,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int completeOwned(
            @Param("taskId") Long taskId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("chunkCount") Integer chunkCount,
            @Param("tokenCount") Integer tokenCount,
            @Param("resultJson") String resultJson,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int failOwned(
            @Param("taskId") Long taskId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("message") String message,
            @Param("now") LocalDateTime now
    );
}
