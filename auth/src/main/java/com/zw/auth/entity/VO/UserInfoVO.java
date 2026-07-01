package com.zw.auth.entity.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private Long tenantId;

    private String roleCode;

    private Byte status;
}