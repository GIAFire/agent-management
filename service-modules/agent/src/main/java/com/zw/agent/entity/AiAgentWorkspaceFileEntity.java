package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.zw.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Agent工作区文件表
 * </p>
 *
 * @author 智纬
 * @since 2026-07-05
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_agent_workspace_file")
public class AiAgentWorkspaceFileEntity extends BaseEntity {

    /**
     * 工作区文件ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * Agent配置版本ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 工具调用ID
     */
    @TableField("tool_call_id")
    private String toolCallId;

    /**
     * 文件后缀
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 展示文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 相对workspace_path的路径
     */
    @TableField("relative_path")
    private String relativePath;

    /**
     * LOCAL/OSS/MINIO/S3
     */
    @TableField("storage_backend")
    private String storageBackend;

    /**
     * 真实存储key，本地可存绝对路径或相对路径
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件扩展名
     */
    @TableField("file_ext")
    private String fileExt;

    /**
     * 文件大小
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * 文件SHA256
     */
    @TableField("checksum")
    private String checksum;

    /**
     * 文件标题
     */
    @TableField("title")
    private String title;

    /**
     * 文件摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 来源：AGENT/TOOL/USER_UPLOAD/SYSTEM
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * PRIVATE/SESSION/AGENT/TENANT
     */
    @TableField("visibility")
    private String visibility;
}
