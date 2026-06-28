package com.zw.auth.entity.VO;

import com.zw.auth.entity.DTO.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private long expiresAt;
    private UserInfoDTO user;
}
