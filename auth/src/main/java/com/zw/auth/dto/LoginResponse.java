package com.zw.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        long expiresAt,
        UserInfo user
) {
    public record UserInfo(Long id, String userName, Long tenantId) {
    }
}
