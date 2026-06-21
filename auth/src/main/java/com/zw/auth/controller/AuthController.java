package com.zw.auth.controller;

import com.zw.auth.config.JwtProperties;
import com.zw.auth.dto.LoginRequest;
import com.zw.auth.dto.LoginResponse;
import com.zw.auth.entity.SysUserEntity;
import com.zw.auth.service.SysUserService;
import com.zw.common.entity.Result;
import com.zw.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final SysUserService sysUserService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("登录参数不能为空");
        }
        SysUserEntity user = sysUserService.authenticate(request.userName(), request.password());
        long expiresAt = Instant.now().plusSeconds(jwtProperties.getExpireSeconds()).toEpochMilli();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userName", user.getUserName());
        claims.put("tenantId", user.getTenantId());

        String token = JwtUtils.createToken(
                String.valueOf(user.getId()),
                jwtProperties.getIssuer(),
                jwtProperties.getSecret(),
                jwtProperties.getExpireSeconds(),
                claims
        );
        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                jwtProperties.getExpireSeconds(),
                expiresAt,
                new LoginResponse.UserInfo(user.getId(), user.getUserName(), user.getTenantId())
        );
        return Result.ok(response);
    }
}
