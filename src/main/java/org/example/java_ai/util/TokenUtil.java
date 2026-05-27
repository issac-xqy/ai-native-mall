package org.example.java_ai.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 简易 Token 工具类
 * 
 * Token 格式: Bearer base64(userId:timestamp)
 * 示例: Bearer MTowMTcwMDAwMDAwMDAw
 */
@Slf4j
public class TokenUtil {

    private static final String PREFIX = "Bearer ";

    /**
     * 生成 Token
     * @param userId 用户ID
     * @return Token字符串
     */
    public static String generateToken(Long userId) {
        long timestamp = System.currentTimeMillis();
        String payload = userId + ":" + timestamp;
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return PREFIX + encoded;
    }

    /**
     * 从 Token 中解析用户ID
     * @param token Token字符串
     * @return 用户ID，解析失败返回null
     */
    public static Long parseUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            // 移除 Bearer 前缀
            String encoded = token;
            if (token.startsWith(PREFIX)) {
                encoded = token.substring(PREFIX.length());
            }

            // 解码
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            
            // 解析 userId:timestamp
            String[] parts = decoded.split(":");
            if (parts.length == 2) {
                return Long.parseLong(parts[0]);
            }
        } catch (Exception e) {
            log.warn("Token解析失败: {}", token);
        }

        return null;
    }

    /**
     * 验证 Token 是否有效（7天过期）
     * @param token Token字符串
     * @return 是否有效
     */
    public static boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String encoded = token;
            if (token.startsWith(PREFIX)) {
                encoded = token.substring(PREFIX.length());
            }

            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            
            if (parts.length == 2) {
                long timestamp = Long.parseLong(parts[1]);
                long now = System.currentTimeMillis();
                long sevenDays = 7L * 24 * 60 * 60 * 1000;
                
                return (now - timestamp) < sevenDays;
            }
        } catch (Exception e) {
            log.warn("Token验证失败: {}", token);
        }

        return false;
    }
}
