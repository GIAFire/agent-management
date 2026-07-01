package com.zw.common.constant;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CacheKeyBuilder {

    // ========== 缓存前缀常量 ==========
    public static final String AGENT_PREFIX = "agent:";
    public static final String USER_PREFIX = "user:";
    public static final String SESSION_PREFIX = "session:";
    public static final String CONFIG_PREFIX = "config:";

    // ========== 缓存过期时间（秒） ==========
    public static final int EXPIRE_5_MIN = 300;
    public static final int EXPIRE_30_MIN = 1800;
    public static final int EXPIRE_1_HOUR = 3600;
    public static final int EXPIRE_24_HOUR = 86400;

    /**
     * 构建Agent缓存Key
     */
    public String buildAgentKey(Long agentId, Long tenantId,Long userId, Long configId) {
        validateParams(agentId, tenantId, userId, configId);
        return String.format("%s%s:%s:%s:%s",
                AGENT_PREFIX,agentId, tenantId, userId, configId);
    }

    /**
     * 构建用户缓存Key
     */
    public String buildUserKey(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return USER_PREFIX + userId;
    }

    /**
     * 构建会话缓存Key
     */
    public String buildSessionKey(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        return SESSION_PREFIX + sessionId;
    }

    /**
     * 批量构建Key（用于批量操作）
     */
    public List<String> buildAgentKeys(List<Long> agentIds) {
        return agentIds.stream()
                .map(id -> AGENT_PREFIX + id)
                .collect(Collectors.toList());
    }

    /**
     * 参数校验
     */
    private void validateParams(Object... params) {
        for (Object param : params) {
            if (param == null) {
                throw new IllegalArgumentException("缓存Key参数不能为null");
            }
        }
    }
}