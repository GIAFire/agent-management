package com.zhiran.auth.entity.VO;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password")
public class LoginRequest {
    private String userName;
    private String password;
}
