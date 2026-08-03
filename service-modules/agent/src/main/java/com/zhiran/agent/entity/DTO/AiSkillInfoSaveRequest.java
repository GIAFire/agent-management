package com.zhiran.agent.entity.DTO;

import lombok.Data;

import java.util.List;

@Data
public class AiSkillInfoSaveRequest {

    private Long id;

    private String name;

    private String skillName;

    private String skillKey;

    private String description;

    private String skillContent;

    private String skillMdContent;

    private String source;

    private String riskLevel;

    private List<String> tags;

    private Byte status;

    private String category;

    private List<String> roleCodes;
}
