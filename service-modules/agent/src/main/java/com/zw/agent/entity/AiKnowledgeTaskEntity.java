package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 持久化知识后台任务。任务记录是一次执行快照，失败后通过新任务重提。
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_task")
public class AiKnowledgeTaskEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("retry_of_task_id")
    private Long retryOfTaskId;

    @TableField("task_type")
    private String taskType;

    @TableField("status")
    private String status;

    @TableField("stage")
    private String stage;

    @TableField("progress")
    private Integer progress;

    @TableField("completed_units")
    private Integer completedUnits;

    @TableField("total_units")
    private Integer totalUnits;

    @TableField("chunk_strategy")
    private String chunkStrategy;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    @TableField("chunk_delimiter")
    private String chunkDelimiter;

    @TableField("request_json")
    private String requestJson;

    @TableField("result_json")
    private String resultJson;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("lease_owner")
    private String leaseOwner;

    @TableField("lease_until")
    private LocalDateTime leaseUntil;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
