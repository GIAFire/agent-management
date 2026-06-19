package com.zw.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MallAdminUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private String mobile;
    private String email;
    private Long roleId;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
