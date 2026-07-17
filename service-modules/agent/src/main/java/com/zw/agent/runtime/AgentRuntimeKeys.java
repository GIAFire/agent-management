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

    public static String buildAgentKey(Long agentId, Long userId, Long configId) {
        validateParams(agentId, userId, configId);
        return String.format("%s%s:%s:%s",
                AGENT_PREFIX,agentId, userId, configId);
    }

    public static String redisStateKey(Long userId, Long sessionId) {
        return "agent-state-session_"
                + userId
                + "-"
                + sessionId;
    }

    private static void validateParams(Object... params) {
        for (Object param : params) {
            if (param == null) {
                throw new IllegalArgumentException("缓存Key参数不能为null");
            }
        }
    }
}
