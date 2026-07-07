package com.zw.auth.entity.DTO;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoDTO {

    private Long id;

    private Long tenantId;

    private String userName;

    private String password;

    private List<String> roleCodes;

    private Byte status;
}
