package com.zw.common.context;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo  implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private Long tenantId;

    private Long roleCode;

    private Byte status;
}
