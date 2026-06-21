package com.zw.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class JwtUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private JwtUtils() {
    }

    public static String createToken(
            String subject,
            String issuer,
            String secret,
            long expireSeconds,
            Map<String, Object> customClaims
    ) {
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("JWT 过期时间必须大于 0");
        }
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = new HashMap<>();
        if (customClaims != null) {
            payload.putAll(customClaims);
        }
        payload.put("sub", subject);
        payload.put("iss", issuer);
        payload.put("iat", now);
        payload.put("exp", now + expireSeconds);

        try {
            String encodedHeader = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(OBJECT_MAPPER.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;
            String signature = sign(unsignedToken, secret);
            return unsignedToken + "." + signature;
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT 生成失败", e);
        }
    }

    public static Map<String, Object> parseToken(String token, String secret, String issuer) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token 格式错误");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken, secret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Token 签名无效");
        }

        try {
            Map<String, Object> claims = OBJECT_MAPPER.readValue(
                    URL_DECODER.decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long exp = longClaim(claims, "exp");
            if (exp <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Token 已过期");
            }
            Object tokenIssuer = claims.get("iss");
            if (issuer != null && !issuer.isBlank() && !issuer.equals(tokenIssuer)) {
                throw new IllegalArgumentException("Token 签发方无效");
            }
            return claims;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token 解析失败", e);
        }
    }

    private static String sign(String content, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT 密钥不能为空");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            return base64UrlEncode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT 签名失败", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return URL_ENCODER.encodeToString(bytes);
    }

    private static long longClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Token 缺少 " + key + " 声明");
    }
}
