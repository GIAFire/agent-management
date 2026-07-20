package com.zw.agent.entity.DTO;

import lombok.Data;

@Data
public class AiSkillResourceSaveRequest {

    private Long id;

    private Long skillId;

    private String fileRole;

    private String fileName;

    private String resourceContent;

    private String resourcePath;

    private String content;

    private String parentPath;

    private String relativePath;

    private Boolean directory;
}
