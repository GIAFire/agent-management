package com.zhiran.agent.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiran.agent.entity.AiKnowledgeDocumentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiKnowledgeDocumentMapper
        extends BaseMapper<AiKnowledgeDocumentEntity> {

    @Select("""
            SELECT *
            FROM ai_knowledge_document
            WHERE id = #{documentId}
              AND deleted = 0
            FOR UPDATE
            """)
    AiKnowledgeDocumentEntity selectByIdForUpdate(
            @Param("documentId") Long documentId
    );

    @InterceptorIgnore(tenantLine = "true")
    List<AiKnowledgeDocumentEntity> findPendingRecoveryCandidates(
            @Param("limit") int limit
    );

    @InterceptorIgnore(tenantLine = "true")
    int claimIndex(
            @Param("documentId") Long documentId,
            @Param("workerId") String workerId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int claimDelete(
            @Param("documentId") Long documentId,
            @Param("workerId") String workerId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    AiKnowledgeDocumentEntity selectWorkerDocument(
            @Param("documentId") Long documentId
    );

    @InterceptorIgnore(tenantLine = "true")
    List<AiKnowledgeDocumentEntity> findExpiredRecoveryCandidates(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @InterceptorIgnore(tenantLine = "true")
    int reclaimExpired(
            @Param("documentId") Long documentId,
            @Param("workerId") String workerId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int heartbeat(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("parseStatus") String parseStatus,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int completeIndexOwned(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("chunkCount") int chunkCount,
            @Param("tokenCount") int tokenCount,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int completeDeleteOwned(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int failIndexOwned(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("message") String message,
            @Param("now") LocalDateTime now
    );

    @InterceptorIgnore(tenantLine = "true")
    int failDeleteOwned(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId,
            @Param("workerId") String workerId,
            @Param("message") String message,
            @Param("now") LocalDateTime now
    );
}
