package com.zw.agent.entity.DTO;

import lombok.Data;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Data
public class FileDTO{


    /**
     * Skill文件ID
     */
    private Long id;

    /**
     * SkillID
     */
    private Long skillId;

    /**
     * 文件角色：SKILL_MD主文件，REFERENCE参考资料，SCRIPT脚本，EXAMPLE样例，ASSET资源
     */
    private String fileRole;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件内容
     */
    private String resourceContent;

    /**
     * 外部存储key；DB存储时可为空
     */
    private String storageKey;

    /**
     * 关联workspace_file或对象存储文件
     */
    private Long workspaceFileId;

    /**
     * 相对Skill目录路径，例如references/style-guide.md、scripts/run-checks.sh
     */
    private String resourcePath;

    /**
     * 文件大小
     */
    private Long sizeBytes;
}
