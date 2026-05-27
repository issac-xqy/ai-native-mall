package org.example.java_ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenUtil 单元测试")
class TokenUtilTest {

    // ==================== generateToken ====================

    @Test
    @DisplayName("generateToken-正常userId-生成包含Bearer前缀的token")
    void generateToken_ValidUserId_ReturnsBearerToken() {
        String token = TokenUtil.generateToken(1L);

        assertNotNull(token);
        assertTrue(token.startsWith("Bearer "));
        assertTrue(token.length() > "Bearer ".length());
    }

    @Test
    @DisplayName("generateToken-不同userId-生成不同token")
    void generateToken_DifferentUserIds_DifferentTokens() {
        String token1 = TokenUtil.generateToken(1L);
        String token2 = TokenUtil.generateToken(2L);

        assertNotEquals(token1, token2);
    }

    // ==================== parseUserId ====================

    @Test
    @DisplayName("parseUserId-有效token-正确解析userId")
    void parseUserId_ValidToken_ReturnsUserId() {
        String token = TokenUtil.generateToken(42L);

        Long userId = TokenUtil.parseUserId(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("parseUserId-null输入-返回null")
    void parseUserId_NullToken_ReturnsNull() {
        assertNull(TokenUtil.parseUserId(null));
    }

    @Test
    @DisplayName("parseUserId-空字符串-返回null")
    void parseUserId_EmptyToken_ReturnsNull() {
        assertNull(TokenUtil.parseUserId(""));
    }

    @Test
    @DisplayName("parseUserId-无Bearer前缀-也能解析")
    void parseUserId_NoBearerPrefix_StillParses() {
        String token = TokenUtil.generateToken(7L);
        String withoutPrefix = token.substring("Bearer ".length());

        Long userId = TokenUtil.parseUserId(withoutPrefix);

        assertEquals(7L, userId);
    }

    @Test
    @DisplayName("parseUserId-非法base64-返回null")
    void parseUserId_InvalidBase64_ReturnsNull() {
        Long userId = TokenUtil.parseUserId("Bearer !!!invalid!!!");
        assertNull(userId);
    }

    @Test
    @DisplayName("parseUserId-格式错误(缺少冒号)-返回null")
    void parseUserId_MalformedPayload_ReturnsNull() {
        String badToken = "Bearer " + java.util.Base64.getEncoder()
                .encodeToString("justOnePart".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Long userId = TokenUtil.parseUserId(badToken);

        assertNull(userId);
    }

    // ==================== isTokenValid ====================

    @Test
    @DisplayName("isTokenValid-刚生成的token-返回true")
    void isTokenValid_FreshToken_ReturnsTrue() {
        String token = TokenUtil.generateToken(1L);

        assertTrue(TokenUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid-null输入-返回false")
    void isTokenValid_NullToken_ReturnsFalse() {
        assertFalse(TokenUtil.isTokenValid(null));
    }

    @Test
    @DisplayName("isTokenValid-空字符串-返回false")
    void isTokenValid_EmptyToken_ReturnsFalse() {
        assertFalse(TokenUtil.isTokenValid(""));
    }

    @Test
    @DisplayName("isTokenValid-过期token(8天前)-返回false")
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        long eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000;
        String payload = "1:" + eightDaysAgo;
        String expiredToken = "Bearer " + java.util.Base64.getEncoder()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertFalse(TokenUtil.isTokenValid(expiredToken));
    }

    @Test
    @DisplayName("isTokenValid-6天前的token-仍在有效期内")
    void isTokenValid_SixDaysOld_ReturnsTrue() {
        long sixDaysAgo = System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000;
        String payload = "1:" + sixDaysAgo;
        String validToken = "Bearer " + java.util.Base64.getEncoder()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertTrue(TokenUtil.isTokenValid(validToken));
    }

    @Test
    @DisplayName("isTokenValid-非法token-返回false")
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(TokenUtil.isTokenValid("garbage"));
    }

    // ==================== 往返测试 ====================

    @Test
    @DisplayName("往返测试-生成token后解析-得到原始userId")
    void roundTrip_GenerateAndParse_OriginalUserId() {
        Long original = 100L;
        String token = TokenUtil.generateToken(original);
        Long parsed = TokenUtil.parseUserId(token);

        assertEquals(original, parsed);
    }
}
