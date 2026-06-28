package com.zw.auth.entity.VO;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long userId;

    private Long tenantId;

    private String userName;

    private String roleCode;

    private Byte status;
}