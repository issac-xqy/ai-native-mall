package org.example.java_ai.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类 — 双 Token 机制
 * Access Token: 15分钟有效期，claim type=access
 * Refresh Token: 7天有效期，claim type=refresh
 */
@Slf4j
public class TokenUtil {

    private static String SECRET = System.getProperty("jwt.secret",
            "AiNativeMall2026-JWT-SecretKey-MustBeAtLeast256BitsForHS256!!");
    private static volatile SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long ACCESS_EXPIRATION_MS = 15L * 60 * 1000;
    private static final long REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;
    private static final String PREFIX = "Bearer ";

    static {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            SECRET = envSecret;
            KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ==================== 生成 ====================

    public static String generateToken(Long userId) {
        return generateAccessToken(userId);
    }

    public static String generateAccessToken(Long userId) {
        Date now = new Date();
        return PREFIX + Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static String generateRefreshToken(Long userId) {
        Date now = new Date();
        return PREFIX + Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static long getAccessExpirationSeconds() {
        return ACCESS_EXPIRATION_MS / 1000;
    }

    // ==================== 解析 ====================

    public static Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("Token subject 不是有效的 userId: {}", claims.getSubject());
            return null;
        }
    }

    public static boolean isTokenValid(String token) {
        Claims claims = parseClaims(token);
        return claims != null && "access".equals(claims.get("type"));
    }

    public static boolean isRefreshToken(String token) {
        Claims claims = parseClaimsAllowExpired(token);
        return claims != null && "refresh".equals(claims.get("type"));
    }

    // ==================== 内部解析方法 ====================

    /**
     * 解析 token，过期返回 null（用于 access token 校验）
     */
    private static Claims parseClaims(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            String jwt = token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
            return Jwts.parser().verifyWith(KEY).build()
                    .parseSignedClaims(jwt).getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期");
            return null;
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Token 签名无效或格式错误");
            return null;
        } catch (Exception e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 token，过期也返回 claims（用于 refresh token 校验）
     */
    private static Claims parseClaimsAllowExpired(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            String jwt = token.startsWith(PREFIX) ? token.substring(PREFIX.length()) : token;
            return Jwts.parser().verifyWith(KEY).build()
                    .parseSignedClaims(jwt).getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期（允许用于 refresh）");
            return null;
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Token 签名无效或格式错误");
            return null;
        } catch (Exception e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
