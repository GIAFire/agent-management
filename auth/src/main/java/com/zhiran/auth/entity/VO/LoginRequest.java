package com.zhiran.auth.entity.VO;

import lombok.Data;

@Data
public class LoginRequest {
    private String userName;
    private String password;
}
