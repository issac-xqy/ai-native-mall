package org.example.java_ai.dto;

/**
 * 登录/刷新结果 — 同时返回 access token 和 refresh token
 */
public record LoginResult(String accessToken, String refreshToken) {
}
