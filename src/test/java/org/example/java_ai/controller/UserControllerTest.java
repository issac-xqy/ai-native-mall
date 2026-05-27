package org.example.java_ai.controller;

import org.example.java_ai.util.TokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController MockMvc 测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String validToken = TokenUtil.generateToken(1L);

    // ==================== POST /api/user/login ====================

    @Test
    @DisplayName("登录-正确用户名密码-返回token")
    void login_CorrectCredentials_ReturnsToken() throws Exception {
        List<Map<String, Object>> rows = List.of(
                userRow(1L, "testuser", encoder.encode("pass123"), "13800138000", "测试用户", "t@t.com"));
        doReturn(rows).when(jdbcTemplate).queryForList(anyString(), eq("testuser"), eq("testuser"));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"testuser","password":"pass123"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userInfo.username").value("testuser"));
    }

    @Test
    @DisplayName("登录-用户不存在-返回失败")
    void login_UserNotFound_ReturnsFail() throws Exception {
        doReturn(Collections.emptyList()).when(jdbcTemplate)
                .queryForList(anyString(), eq("nobody"), eq("nobody"));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"nobody","password":"pass"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("登录-密码错误-返回失败")
    void login_WrongPassword_ReturnsFail() throws Exception {
        List<Map<String, Object>> rows = List.of(
                userRow(1L, "testuser", encoder.encode("correct"), "13800138000", "用户", "t@t.com"));
        doReturn(rows).when(jdbcTemplate).queryForList(anyString(), eq("testuser"), eq("testuser"));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"testuser","password":"wrong_pwd"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    // ==================== POST /api/user/register ====================

    @Test
    @DisplayName("注册-新用户-注册成功")
    void register_NewUser_Success() throws Exception {
        doReturn(0).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class), eq("newuser"));
        doReturn(0).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class), eq("13800009999"));
        doReturn(1).when(jdbcTemplate)
                .update(anyString(), eq("newuser"), eq("13800009999"), eq("new@test.com"), anyString(), eq("newuser"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"newuser","password":"pass123","phone":"13800009999","email":"new@test.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("注册-用户名已存在-返回失败")
    void register_DuplicateUsername_ReturnsFail() throws Exception {
        doReturn(1).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class), eq("existing"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"existing","password":"pass","phone":"13800000001","email":"e@t.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("注册-手机号已存在-返回失败")
    void register_DuplicatePhone_ReturnsFail() throws Exception {
        doReturn(0).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class), eq("newuser"));
        doReturn(1).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class), eq("13800000001"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"newuser","password":"pass","phone":"13800000001","email":"e@t.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("手机号已被注册"));
    }

    // ==================== GET /api/user/info ====================

    @Test
    @DisplayName("获取用户信息-已登录-返回用户数据")
    void getUserInfo_Authenticated_ReturnsUser() throws Exception {
        List<Map<String, Object>> rows = List.of(
                userRow(1L, "testuser", "enc", "13800138000", "测试", "t@t.com"));
        doReturn(rows).when(jdbcTemplate).queryForList(anyString(), eq(1L));

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("获取用户信息-无Token-拦截器返回401")
    void getUserInfo_NoToken_InterceptorReturns401() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/user/info ====================

    @Test
    @DisplayName("更新用户信息-已登录-更新成功")
    void updateUserInfo_Authenticated_Success() throws Exception {
        doReturn(1).when(jdbcTemplate)
                .update(anyString(), eq("新昵称"), eq("13800008888"), eq("new@test.com"), eq(1L));

        mockMvc.perform(put("/api/user/info")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nickname":"新昵称","phone":"13800008888","email":"new@test.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    @DisplayName("更新用户信息-无Token-拦截器返回401")
    void updateUserInfo_NoToken_InterceptorReturns401() throws Exception {
        mockMvc.perform(put("/api/user/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取用户信息-无效Token-返回401")
    void getUserInfo_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> userRow(Long id, String username, String password,
                                        String phone, String nickname, String email) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("username", username);
        row.put("password", password);
        row.put("phone", phone);
        row.put("nickname", nickname);
        row.put("email", email);
        return row;
    }
}
