package com.zhiran.auth.entity.VO;

import com.zhiran.common.context.UserInfo;
import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@ToString(exclude = "token")
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private long expiresAt;
    private UserInfo user;
}
