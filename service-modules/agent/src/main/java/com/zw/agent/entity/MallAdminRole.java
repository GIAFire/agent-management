package com.zw.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MallAdminRole {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
