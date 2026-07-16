package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.common.entity.BaseEntity;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 知识库文档表：记录平台文档与外部RAG/向量库文档的映射关系
 * </p>
 *
 * @author 智纬
 * @since 2026-07-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_knowledge_document")
public class AiKnowledgeDocumentEntity extends BaseEntity {

    /**
     * 知识库文档主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 知识库ID，关联ai_knowledge_base.id
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 来源工作区文件ID，关联ai_agent_workspace_file.id
     */
    @TableField("workspace_file_id")
    private Long workspaceFileId;

    /**
     * 外部文档ID，例如RAGFlow document_id、向量库文档ID
     */
    @TableField("external_document_id")
    private String externalDocumentId;

    /**
     * 文档名称，用于展示和检索
     */
    @TableField("document_name")
    private String documentName;

    /**
     * 文档类型：PDF/DOCX/XLSX/TXT/MD/HTML/CSV/JSON/URL
     */
    @TableField("document_type")
    private String documentType;

    /**
     * MIME类型，例如application/pdf、text/markdown
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 来源类型：WORKSPACE_FILE/UPLOAD/URL/API/MANUAL
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源地址，例如文件路径、URL、对象存储key
     */
    @TableField("source_uri")
    private String sourceUri;

    /**
     * 文档大小，单位字节
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * 文档内容校验值，例如SHA256
     */
    @TableField("checksum")
    private String checksum;

    /**
     * 文档语言，例如zh-CN、en-US
     */
    @TableField("language")
    private String language;

    /**
     * 文档版本号，重新上传或重建时递增
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 解析状态：PENDING待处理、UPLOADED已上传、PARSING解析中、CHUNKING切片中、EMBEDDING向量化中、READY可检索、FAILED失败
     */
    @TableField("parse_status")
    private String parseStatus;

    /**
     * 切片数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 文档估算token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 处理失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 后端特定元信息，例如RAGFlow解析进度、缩略图、OCR配置
     */
    @TableField("provider_meta_json")
    private String providerMetaJson;

    /**
     * 状态：1启用，0禁用，2删除
     */
    @TableField("status")
    private Byte status;
}
