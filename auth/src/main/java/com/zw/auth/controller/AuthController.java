package com.zw.auth.controller;

import com.zw.auth.entity.DTO.UserInfoDTO;
import com.zw.auth.entity.VO.LoginRequest;
import com.zw.auth.entity.VO.LoginResponse;
import com.zw.auth.entity.VO.UserInfoVO;
import com.zw.auth.service.SysUserService;
import com.zw.common.RedisService;
import com.zw.common.config.JwtProperties;
import com.zw.common.constant.RedisConstants;
import com.zw.common.entity.Result;
import com.zw.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final SysUserService sysUserService;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("登录参数不能为空");
        }
        UserInfoDTO user = sysUserService.authenticate(request.getUserName(), request.getPassword());
        long expiresAt = Instant.now().plusSeconds(jwtProperties.getExpireSeconds()).toEpochMilli();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userName", user.getUserName());
        claims.put("tenantId", user.getTenantId());

        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setUserId(user.getId());
        String token = JwtUtils.createToken(
                String.valueOf(user.getId()),
                jwtProperties.getIssuer(),
                jwtProperties.getSecret(),
                jwtProperties.getExpireSeconds(),
                claims
        );
        redisService.setCacheObject(RedisConstants.SESSION + token, userInfo,jwtProperties.getExpireSeconds(), TimeUnit.SECONDS);
        LoginResponse loginResponse = new LoginResponse(token,
                 "Bearer",
                jwtProperties.getExpireSeconds(),
                expiresAt,
                userInfo);
        return Result.ok(loginResponse);
    }
}
