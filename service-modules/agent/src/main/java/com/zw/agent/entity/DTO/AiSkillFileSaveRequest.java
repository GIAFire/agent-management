package com.zw.agent.entity.DTO;

import lombok.Data;

@Data
public class AiSkillFileSaveRequest {

    private Long id;

    private Long skillId;

    private String parentPath;

    private String fileName;

    private String relativePath;

    private String fileRole;

    private String content;

    private Boolean directory;

    private Byte executable;
}
