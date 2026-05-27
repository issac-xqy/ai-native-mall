package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.User;
import org.example.java_ai.util.TokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            log.info("🔐 用户登录请求 - 用户名: {}", username);

            // 数据库验证（已启用）
            String sql = "SELECT * FROM sys_user WHERE (username = ? OR phone = ?) AND deleted = 0";
            java.util.List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username, username);

            if (users.isEmpty()) {
                log.warn("❌ 登录失败 - 用户不存在: {}", username);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "用户名或密码错误"
                ));
            }

            Map<String, Object> user = users.get(0);
            
            String storedPassword = (String) user.get("password");
            if (!passwordEncoder.matches(password, storedPassword)) {
                log.warn("❌ 登录失败 - 密码错误: {}", username);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "用户名或密码错误"
                ));
            }
            
            Long userId = ((Number) user.get("id")).longValue();
            String token = TokenUtil.generateToken(userId);
            
            log.info("✅ 用户登录成功 - 用户名: {}, 用户ID: {}, Token已生成", username, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登录成功",
                "token", token,
                "userInfo", Map.of(
                    "id", userId,
                    "username", user.get("username"),
                    "phone", user.get("phone"),
                    "nickname", user.get("nickname") != null ? user.get("nickname") : user.get("username"),
                    "email", user.get("email") != null ? user.get("email") : ""
                )
            ));

        } catch (Exception e) {
            log.error("❌ 用户登录异常", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "登录失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String phone = request.get("phone");
            String email = request.get("email");
            String password = request.get("password");

            log.info("📝 用户注册请求 - 用户名: {}, 手机号: {}", username, phone);

            // 数据库注册（已启用）
            // 检查用户名是否已存在
            String checkUsernameSql = "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0";
            Integer usernameCount = jdbcTemplate.queryForObject(checkUsernameSql, Integer.class, username);
            if (usernameCount != null && usernameCount > 0) {
                log.warn("❌ 注册失败 - 用户名已存在: {}", username);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "用户名已存在"
                ));
            }

            // 检查手机号是否已存在
            String checkPhoneSql = "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND deleted = 0";
            Integer phoneCount = jdbcTemplate.queryForObject(checkPhoneSql, Integer.class, phone);
            if (phoneCount != null && phoneCount > 0) {
                log.warn("❌ 注册失败 - 手机号已被注册: {}", phone);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "手机号已被注册"
                ));
            }

            // 加密密码
            String encodedPassword = passwordEncoder.encode(password);

            // 插入用户
            String insertSql = """
                INSERT INTO sys_user (username, phone, email, password, nickname, status, create_time, update_time) 
                VALUES (?, ?, ?, ?, ?, 1, NOW(), NOW())
                """;
            
            jdbcTemplate.update(insertSql, username, phone, email, encodedPassword, username);
            
            log.info("✅ 用户注册成功 - 用户名: {}", username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "注册成功，请使用该账号登录"
            ));

        } catch (Exception e) {
            log.error("❌ 用户注册异常", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "注册失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未登录"
                ));
            }

            // 从 Request 属性中获取用户ID（由拦截器设置）
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未登录"
                ));
            }

            String sql = "SELECT id, username, phone, email, nickname, avatar, create_time FROM sys_user WHERE id = ? AND deleted = 0";
            java.util.List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "用户不存在"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", users.get(0)
            ));

        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public ResponseEntity<Map<String, Object>> updateUserInfo(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> request) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未登录"
                ));
            }

            // 从 Request 属性中获取用户ID（由拦截器设置）
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未登录"
                ));
            }

            String nickname = request.get("nickname");
            String phone = request.get("phone");
            String email = request.get("email");

            log.info("更新用户信息 - 用户ID: {}", userId);

            String sql = "UPDATE sys_user SET nickname = ?, phone = ?, email = ?, update_time = NOW() WHERE id = ? AND deleted = 0";
            int rows = jdbcTemplate.update(sql, nickname, phone, email, userId);

            if (rows > 0) {
                log.info("✅ 用户信息更新成功");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "更新成功"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "更新失败"
                ));
            }
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "更新失败: " + e.getMessage()
            ));
        }
    }
}
