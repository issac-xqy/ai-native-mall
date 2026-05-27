package org.example.java_ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenUtil 单元测试")
class TokenUtilTest {

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
    @DisplayName("parseUserId-非法token-返回null")
    void parseUserId_InvalidToken_ReturnsNull() {
        assertNull(TokenUtil.parseUserId("Bearer !!!invalid!!!"));
    }

    @Test
    @DisplayName("parseUserId-伪造token-返回null")
    void parseUserId_ForgedToken_ReturnsNull() {
        // JWT 带签名的伪造 token，parseUserId 应返回 null
        String forged = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTkifQ.fake";
        assertNull(TokenUtil.parseUserId(forged));
    }

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
    @DisplayName("isTokenValid-非法token-返回false")
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(TokenUtil.isTokenValid("garbage"));
    }

    @Test
    @DisplayName("isTokenValid-伪造token-返回false")
    void isTokenValid_ForgedToken_ReturnsFalse() {
        String forged = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.forged_signature";
        assertFalse(TokenUtil.isTokenValid(forged));
    }

    @Test
    @DisplayName("parseUserId-伪造的base64 token-返回null")
    void parseUserId_ForgedBase64Token_ReturnsNull() {
        String badToken = "Bearer " + java.util.Base64.getEncoder()
                .encodeToString("123:99999999".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertNull(TokenUtil.parseUserId(badToken));
    }

    @Test
    @DisplayName("往返测试-生成token后解析-得到原始userId")
    void roundTrip_GenerateAndParse_OriginalUserId() {
        Long original = 100L;
        String token = TokenUtil.generateToken(original);
        Long parsed = TokenUtil.parseUserId(token);
        assertEquals(original, parsed);
        assertTrue(TokenUtil.isTokenValid(token));
    }
}
