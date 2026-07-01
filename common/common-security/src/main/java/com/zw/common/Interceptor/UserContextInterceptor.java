package com.zw.common.Interceptor;

import com.zw.common.RedisService;
import com.zw.common.config.JwtProperties;
import com.zw.common.constant.RedisConstants;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final RedisService redisService;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        String token = getToken(request);

        if (token == null || token.isBlank()) {
            return true;
        }

        // 1. 先校验 JWT 是否合法、是否过期
        try {
            JwtUtils.parseToken(token, jwtProperties.getSecret(),jwtProperties.getIssuer());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        UserInfo userInfo = redisService.getCacheObject(
                RedisConstants.SESSION + token,
                UserInfo.class
        );
        if (userInfo != null) {
            UserContext.set(userInfo);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        UserContext.clear();
    }

    private String getToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        if (!authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7);
    }
}