package com.zw.common.Interceptor;

import com.zw.common.RedisService;
import com.zw.common.constant.RedisConstants;
import com.zw.common.constant.UserHeaderConstants;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final RedisService redisService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String userIdStr = request.getHeader(UserHeaderConstants.USER_ID);

        if (userIdStr == null || userIdStr.isBlank()) {
            return true;
        }

        UserInfo userInfo = redisService.getCacheObject(
                RedisConstants.USER_INFO + userIdStr,
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
}