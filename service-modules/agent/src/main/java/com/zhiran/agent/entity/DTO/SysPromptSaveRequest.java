package com.zhiran.agent.entity.DTO;

import lombok.Data;

@Data
public class SysPromptSaveRequest {
    private Long id;
    private String promptName;
    private String description;
    private String sysPrompt;
    private Integer version;
}
