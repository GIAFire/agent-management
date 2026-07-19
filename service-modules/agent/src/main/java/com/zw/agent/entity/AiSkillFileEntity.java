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
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ai_skill_file")
public class AiSkillFileEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Skill文件ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * SkillID
     */
    @TableField("skill_id")
    private Long skillId;

    /**
     * 文件角色：SKILL_MD主文件，REFERENCE参考资料，SCRIPT脚本，EXAMPLE样例，ASSET资源
     */
    @TableField("file_role")
    private String fileRole;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件内容
     */
    @TableField("resource_content")
    private String resourceContent;

    /**
     * 相对Skill目录路径，例如references/style-guide.md、scripts/run-checks.sh
     */
    @TableField("resource_path")
    private String resourcePath;

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
     * 文件SHA256校验值
     */
    @TableField("checksum")
    private String checksum;

    /**
     * 存储后端：DB/LOCAL/MINIO/OSS
     */
    @TableField("storage_backend")
    private String storageBackend;

    /**
     * 外部存储key；DB存储时可为空
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * 关联workspace_file或对象存储文件
     */
    @TableField("workspace_file_id")
    private Long workspaceFileId;

    /**
     * 是否为可执行脚本：1是，0否
     */
    @TableField("executable")
    private Byte executable;
}
