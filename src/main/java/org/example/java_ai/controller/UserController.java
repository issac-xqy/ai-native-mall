package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.User;
import org.example.java_ai.service.UserService;
import org.example.java_ai.util.TokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String token = userService.login(username, password);
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "登录成功", "token", token,
                    "userInfo", Map.of(
                            "id", user.getId(), "username", user.getUsername(),
                            "phone", user.getPhone() != null ? user.getPhone() : "",
                            "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                            "email", user.getEmail() != null ? user.getEmail() : "")));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            userService.register(request.get("username"), request.get("password"),
                    request.get("username"), request.get("phone"), request.get("email"));
            return ResponseEntity.ok(Map.of("success", true, "message", "注册成功，请使用该账号登录"));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpServletRequest servletRequest,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        User user = userService.getById(userId);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                "id", user.getId(), "username", user.getUsername(),
                "phone", user.getPhone(), "nickname", user.getNickname(), "email", user.getEmail())));
    }

    @PutMapping("/info")
    public ResponseEntity<Map<String, Object>> updateUserInfo(HttpServletRequest servletRequest,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        User update = new User();
        update.setNickname(request.get("nickname"));
        update.setPhone(request.get("phone"));
        update.setEmail(request.get("email"));
        userService.updateUserInfo(userId, update);
        return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
    }
}
