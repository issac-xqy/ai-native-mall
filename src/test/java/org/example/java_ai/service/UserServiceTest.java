package org.example.java_ai.service;

import org.example.java_ai.entity.User;
import org.example.java_ai.mapper.UserMapper;
import org.example.java_ai.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("getUserByUsername-用户存在-返回用户")
    void getUserByUsername_Exists_ReturnsUser() {
        User mockUser = buildUser(1L, "zhangsan", "张三", "13800001111");
        doReturn(mockUser).when(userMapper).selectOne(any(), anyBoolean());

        User result = userService.getUserByUsername("zhangsan");

        assertNotNull(result);
        assertEquals("zhangsan", result.getUsername());
    }

    @Test
    @DisplayName("getUserByUsername-用户不存在-返回null")
    void getUserByUsername_NotExists_ReturnsNull() {
        doReturn(null).when(userMapper).selectOne(any(), anyBoolean());

        assertNull(userService.getUserByUsername("nobody"));
    }

    @Test
    @DisplayName("register-新用户名-注册成功")
    void register_NewUsername_Success() {
        doReturn(null).when(userMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(userMapper).insert((User) any());

        User result = userService.register("newuser", "pass123", "新用户", "13800002222", "new@test.com");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertNotEquals("pass123", result.getPassword());
    }

    @Test
    @DisplayName("register-用户名已存在-抛异常")
    void register_DuplicateUsername_ThrowsException() {
        doReturn(buildUser(1L, "existing", "老用户", "13800003333"))
                .when(userMapper).selectOne(any(), anyBoolean());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.register("existing", "pwd", "昵称", "13800004444", "e@t.com"));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    @DisplayName("login-正确用户名密码且账号启用-返回token")
    void login_CorrectCredentials_ReturnsToken() {
        User mockUser = buildUser(1L, "zhangsan", "张三", "13800001111");
        mockUser.setPassword(encoder.encode("pass123"));
        doReturn(mockUser).when(userMapper).selectOne(any(), anyBoolean());

        String token = userService.login("zhangsan", "pass123");

        assertNotNull(token);
        assertTrue(token.startsWith("mock-jwt-token-"));
    }

    @Test
    @DisplayName("login-用户名不存在-抛异常")
    void login_UserNotFound_ThrowsException() {
        doReturn(null).when(userMapper).selectOne(any(), anyBoolean());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.login("nobody", "pass"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("login-密码错误-抛异常")
    void login_WrongPassword_ThrowsException() {
        User mockUser = buildUser(1L, "zhangsan", "张三", "13800001111");
        mockUser.setPassword(encoder.encode("correct_pwd"));
        doReturn(mockUser).when(userMapper).selectOne(any(), anyBoolean());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.login("zhangsan", "wrong_pwd"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("login-账号被禁用-抛异常")
    void login_DisabledAccount_ThrowsException() {
        User mockUser = buildUser(1L, "zhangsan", "张三", "13800001111");
        mockUser.setPassword(encoder.encode("pass"));
        mockUser.setStatus(0);
        doReturn(mockUser).when(userMapper).selectOne(any(), anyBoolean());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.login("zhangsan", "pass"));
        assertEquals("账号已被禁用", ex.getMessage());
    }

    @Test
    @DisplayName("updateUserInfo-用户存在-更新成功")
    void updateUserInfo_UserExists_UpdatesSuccessfully() {
        User existing = buildUser(1L, "zhangsan", "张三", "13800001111");
        doReturn(existing).when(userMapper).selectById(eq(1L));
        doReturn(1).when(userMapper).updateById((User) any());

        User update = new User();
        update.setNickname("新昵称");
        update.setPhone("13900009999");
        User result = userService.updateUserInfo(1L, update);

        assertEquals("新昵称", result.getNickname());
        assertEquals("13900009999", result.getPhone());
    }

    @Test
    @DisplayName("updateUserInfo-用户不存在-抛异常")
    void updateUserInfo_UserNotExists_ThrowsException() {
        doReturn(null).when(userMapper).selectById(eq(99L));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.updateUserInfo(99L, new User()));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("changePassword-原密码正确-修改成功")
    void changePassword_CorrectOldPassword_Success() {
        User user = buildUser(1L, "zhangsan", "张三", "13800001111");
        user.setPassword(encoder.encode("old_pwd"));
        doReturn(user).when(userMapper).selectById(eq(1L));
        doReturn(1).when(userMapper).updateById((User) any());

        assertTrue(userService.changePassword(1L, "old_pwd", "new_pwd"));
    }

    @Test
    @DisplayName("changePassword-原密码错误-抛异常")
    void changePassword_WrongOldPassword_ThrowsException() {
        User user = buildUser(1L, "zhangsan", "张三", "13800001111");
        user.setPassword(encoder.encode("real_old"));
        doReturn(user).when(userMapper).selectById(eq(1L));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.changePassword(1L, "wrong_old", "new_pwd"));
        assertEquals("原密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("changePassword-用户不存在-抛异常")
    void changePassword_UserNotExists_ThrowsException() {
        doReturn(null).when(userMapper).selectById(eq(99L));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.changePassword(99L, "old", "new"));
        assertEquals("用户不存在", ex.getMessage());
    }

    private User buildUser(Long id, String username, String nickname, String phone) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(username + "@test.com");
        user.setStatus(1);
        return user;
    }
}
