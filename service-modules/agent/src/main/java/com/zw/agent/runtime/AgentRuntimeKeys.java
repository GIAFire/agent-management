package com.zw.agent.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class AgentRuntimeKeys {

    public static final String AGENT_PREFIX = "agent:";


    private AgentRuntimeKeys() {
    }

    public static String buildAgentKey(Long agentId, Long userId, Long configId, Long sessionId) {
        validateParams(agentId, userId, configId,sessionId);
        return String.format("%s%s:%s:%s:%s",
                AGENT_PREFIX,agentId, userId, configId,sessionId);
    }

    public static String userKey(Long userId) {
        return "user_" + userId;
    }

    public static String sessionKey(Long sessionId) {
        return "session_" + sessionId;
    }

    public static String redisStateKey(String runtimeUserKey, String runtimeSessionKey) {
        return "agent-state-session_"
                + runtimeUserKey
                + "-"
                + runtimeSessionKey;
    }

    private static void validateParams(Object... params) {
        for (Object param : params) {
            if (param == null) {
                throw new IllegalArgumentException("缓存Key参数不能为null");
            }
        }
    }
}
