package com.zw.agent.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class AgentRuntimeKeys {

    private AgentRuntimeKeys() {
    }

    public static String userKey(Long tenantId, Long userId) {
        return "tenant_" + tenantId + "-user_" + userId;
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
}
