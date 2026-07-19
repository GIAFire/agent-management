package com.zw.agent.entity.DTO;


import com.zw.agent.entity.AiSkillFileEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkillFileDTO {
    private Long id;

    /**
     * Skill显示名称
     */
    private String name;

    /**
     * Skill描述，用于Agent判断何时使用该Skill
     */
    private String description;

    /**
     * SKILL.md正文
     */
    private String skillContent;

    /**
     * SKILL.md正文
     */
    private String source;

    /**
     * 完整的 YAML 元数据，以 JSON 形式保存
     */
    private String metadataJson;

    /**
     * 风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    private String riskLevel;

    /**
     * 是否需要执行脚本或shell：1需要，0不需要
     */
    private Byte requiresShell;

    /**
     * 是否必须在沙箱中使用：1是，0否
     */
    private Byte requiresSandbox;

    /**
     * 标签JSON数组，例如["代码审查","Java","Spring"]
     */
    private String tagsJson;

    /**
     * 状态：1启用，0停用
     */
    private Byte status;

    private List<FileDTO> skillFileList;
}
