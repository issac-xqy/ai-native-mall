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
 * JWT Token 工具类
 * 使用 HMAC-SHA256 签名，防止伪造
 */
@Slf4j
public class TokenUtil {

    private static String SECRET = System.getProperty("jwt.secret",
            "AiNativeMall2026-JWT-SecretKey-MustBeAtLeast256BitsForHS256!!");
    private static volatile SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;
    private static final String PREFIX = "Bearer ";

    static {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            SECRET = envSecret;
            KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String generateToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);

        return PREFIX + Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY)
                .compact();
    }

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
        return parseClaims(token) != null;
    }

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
}
