package com.example.ems.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresInMs, UserResponse user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInMs, user);
    }
}
