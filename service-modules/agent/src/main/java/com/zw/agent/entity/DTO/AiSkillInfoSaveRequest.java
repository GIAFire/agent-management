package com.zw.agent.entity.DTO;

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

    private String metadataJson;

    private String riskLevel;

    private String tagsJson;

    private Byte status;

    private String category;

    private Byte requiresShell;

    private Byte requiresSandbox;

    private String scopeType;

    private String scopeValue;

    private List<String> roleCodes;

    private List<String> roleIds;
}
