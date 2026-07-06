package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.dto.LoginResult;
import org.example.java_ai.dto.RefreshTokenRequest;
import org.example.java_ai.entity.User;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.service.UserService;
import org.example.java_ai.util.TokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        LoginResult loginResult = userService.login(username, password);
        User user = userService.getUserByUsername(username);
        return Result.success(Map.of(
                "accessToken", loginResult.accessToken(),
                "refreshToken", loginResult.refreshToken(),
                "tokenType", "Bearer",
                "expiresIn", TokenUtil.getAccessExpirationSeconds(),
                "userInfo", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "phone", user.getPhone() != null ? user.getPhone() : "",
                        "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail() : ""
                )));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        LoginResult loginResult = userService.refreshToken(request.refreshToken());
        return Result.success(Map.of(
                "accessToken", loginResult.accessToken(),
                "refreshToken", loginResult.refreshToken(),
                "tokenType", "Bearer",
                "expiresIn", TokenUtil.getAccessExpirationSeconds()));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        userService.register(
                request.get("username"),
                request.get("password"),
                request.get("nickname"),
                request.get("phone"),
                request.get("email"));
        return Result.success(Map.of("message", "注册成功，请使用该账号登录"));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "phone", user.getPhone(),
                "nickname", user.getNickname(),
                "email", user.getEmail()));
    }

    @PutMapping("/info")
    public Result<Map<String, Object>> updateUserInfo(HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        User update = new User();
        update.setNickname(body.get("nickname"));
        update.setPhone(body.get("phone"));
        update.setEmail(body.get("email"));
        userService.updateUserInfo(userId, update);
        return Result.success(Map.of("message", "更新成功"));
    }
}
