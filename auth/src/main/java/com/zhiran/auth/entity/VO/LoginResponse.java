package com.zhiran.auth.entity.VO;

import com.zhiran.common.context.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private long expiresAt;
    private UserInfo user;
}
