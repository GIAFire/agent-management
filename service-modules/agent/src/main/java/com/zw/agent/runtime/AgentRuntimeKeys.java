package com.zw.agent.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class AgentRuntimeKeys {

    private static final int MAX_KEY_LENGTH = 120;

    private AgentRuntimeKeys() {
    }

    public static String userKey(String tenantCode, Long userId) {
        String tenantPart = pathSafeSegment(tenantCode, "tenant");
        String userPart = userId == null
                ? "anonymous"
                : pathSafeSegment(String.valueOf(userId), "anonymous");
        return limitLength("tenant-" + tenantPart + "-user-" + userPart);
    }

    public static String sessionKey(String sessionKey) {
        return pathSafeSegment(sessionKey, "default");
    }

    static String pathSafeSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String trimmed = value.strip();
        StringBuilder key = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); ) {
            int codePoint = trimmed.codePointAt(i);
            if (isSafeAscii(codePoint)) {
                key.appendCodePoint(codePoint);
            } else {
                key.append(encodedCodePoint(codePoint));
            }
            i += Character.charCount(codePoint);
        }

        if (key.isEmpty()) {
            return fallback;
        }
        return limitLength(key.toString());
    }

    private static boolean isSafeAscii(int codePoint) {
        return (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= '0' && codePoint <= '9')
                || codePoint == '-';
    }

    private static String encodedCodePoint(int codePoint) {
        String hex = Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
        return "_" + "0".repeat(Math.max(0, 4 - hex.length())) + hex + "_";
    }

    private static String limitLength(String value) {
        if (value.length() <= MAX_KEY_LENGTH) {
            return value;
        }

        String digest = sha256Hex(value).substring(0, 16);
        int prefixLength = MAX_KEY_LENGTH - digest.length() - 1;
        return value.substring(0, prefixLength) + "-" + digest;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
