package org.example.java_ai.controller;

import org.example.java_ai.dto.LoginResult;
import org.example.java_ai.entity.User;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.service.UserService;
import org.example.java_ai.util.TokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class,
        excludeAutoConfiguration = {
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@DisplayName("UserController MockMvc 测试")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    private final String validToken = TokenUtil.generateAccessToken(1L);

    @Test
    @DisplayName("登录-正确用户名密码-返回token")
    void login_CorrectCredentials_ReturnsToken() throws Exception {
        User mockUser = buildUser(1L, "testuser", "测试");
        when(userService.login("testuser", "pass123"))
                .thenReturn(new LoginResult("Bearer test.access.jwt.token", "Bearer test.refresh.jwt.token"));
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    @DisplayName("登录-用户不存在-返回失败")
    void login_UserNotFound_ReturnsFail() throws Exception {
        when(userService.login("nobody", "pass"))
                .thenThrow(new BusinessException(400, "用户名或密码错误"));

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("注册-新用户-成功")
    void register_NewUser_Success() throws Exception {
        when(userService.register(eq("newuser"), eq("pass123"), any(), eq("13800000001"), eq("e@t.com")))
                .thenReturn(new User());

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"newuser","password":"pass123","nickname":"newuser","phone":"13800000001","email":"e@t.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("注册-用户名已存在-返回失败")
    void register_DuplicateUsername_ReturnsFail() throws Exception {
        when(userService.register(eq("existing"), any(), any(), any(), any()))
                .thenThrow(new BusinessException(400, "用户名已存在"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"existing","password":"pass","nickname":"existing","phone":"13800000001","email":"e@t.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("获取用户信息-已登录-返回用户")
    void getUserInfo_Authenticated_ReturnsUser() throws Exception {
        when(userService.getById(1L)).thenReturn(buildUser(1L, "testuser", "测试"));

        mockMvc.perform(get("/user/info")
                        .requestAttr("userId", 1L)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("获取用户信息-无Token-返回401")
    void getUserInfo_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/user/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("更新用户信息-已登录-成功")
    void updateUserInfo_Authenticated_Success() throws Exception {
        when(userService.updateUserInfo(eq(1L), any(User.class)))
                .thenReturn(buildUser(1L, "testuser", "新昵称"));

        mockMvc.perform(put("/user/info")
                        .requestAttr("userId", 1L)
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nickname":"新昵称","phone":"13800008888","email":"new@test.com"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新用户信息-无Token-返回401")
    void updateUserInfo_NoToken_Returns401() throws Exception {
        mockMvc.perform(put("/user/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private User buildUser(Long id, String username, String nickname) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPhone("13800138000");
        u.setEmail(username + "@test.com");
        u.setStatus(1);
        return u;
    }
}
