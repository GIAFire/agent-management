package com.zw.common.context;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserInfo  implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private Long tenantId;

    private List<String> roleCodes;

    private Byte status;
}
