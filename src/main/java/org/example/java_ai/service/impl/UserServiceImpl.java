package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.config.RedisRateLimiter;
import org.example.java_ai.dto.LoginResult;
import org.example.java_ai.entity.User;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.mapper.UserMapper;
import org.example.java_ai.service.UserService;
import org.example.java_ai.util.TokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisRateLimiter redisRateLimiter;

    private static final int LOGIN_RATE_LIMIT_PER_USERNAME = 5;
    private static final int LOGIN_RATE_LIMIT_PER_IP = 20;
    private static final int LOGIN_RATE_WINDOW_SECONDS = 60;

    @Override
    public User getUserByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User register(String username, String password, String nickname, String phone, String email) {
        User existingUser = getUserByUsername(username);
        if (existingUser != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1);
        save(user);
        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public LoginResult login(String username, String password) {
        // Redis 限流检查：用户名维度 5次/分钟
        String usernameKey = "login:username:" + username;
        String ipKey = "login:ip:" + getClientIp();

        try {
            if (!redisRateLimiter.tryAcquire(usernameKey, LOGIN_RATE_LIMIT_PER_USERNAME, LOGIN_RATE_WINDOW_SECONDS)) {
                long retryAfter = redisRateLimiter.getTtl(usernameKey);
                throw new BusinessException(ResultCode.LOGIN_RATE_LIMITED,
                        "登录尝试过于频繁，请" + retryAfter + "秒后重试",
                        (int) retryAfter);
            }
            if (!redisRateLimiter.tryAcquire(ipKey, LOGIN_RATE_LIMIT_PER_IP, LOGIN_RATE_WINDOW_SECONDS)) {
                long retryAfter = redisRateLimiter.getTtl(ipKey);
                throw new BusinessException(ResultCode.LOGIN_RATE_LIMITED,
                        "登录尝试过于频繁，请" + retryAfter + "秒后重试",
                        (int) retryAfter);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis 限流检查异常，降级放行", e);
            // Redis 不可用时降级放行，避免 Login 完全不可用
        }

        User user = getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号已被禁用");
        }

        // 登录成功，清除限流计数
        try {
            redisRateLimiter.reset(usernameKey);
            redisRateLimiter.reset(ipKey);
        } catch (Exception e) {
            log.debug("清除限流计数失败", e);
        }

        log.info("用户登录成功: {}", username);
        return new LoginResult(
                TokenUtil.generateAccessToken(user.getId()),
                TokenUtil.generateRefreshToken(user.getId()));
    }

    @Override
    public LoginResult refreshToken(String refreshToken) {
        if (!TokenUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        Long userId = TokenUtil.parseUserId(refreshToken);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        User user = getById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已禁用");
        }
        log.info("Token 刷新成功: userId={}", userId);
        return new LoginResult(
                TokenUtil.generateAccessToken(userId),
                TokenUtil.generateRefreshToken(userId));
    }

    @Override
    public User updateUserInfo(Long userId, User user) {
        User existingUser = getById(userId);
        if (existingUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(user.getNickname()) && user.getNickname().length() <= 50) {
            existingUser.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getPhone()) && user.getPhone().matches("^1[3-9]\\d{9}$")) {
            existingUser.setPhone(user.getPhone());
        }
        if (StringUtils.hasText(user.getEmail()) && user.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            existingUser.setEmail(user.getEmail());
        }
        if (StringUtils.hasText(user.getAvatar())) {
            existingUser.setAvatar(user.getAvatar());
        }
        updateById(existingUser);
        log.info("用户信息更新成功: {}", userId);
        return existingUser;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        log.info("用户密码修改成功: {}", userId);
        return true;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public java.util.Map<Long, java.util.Map<String, Object>> getUserMap(
            java.util.Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return java.util.Map.of();
        java.util.Map<Long, java.util.Map<String, Object>> result = new java.util.HashMap<>();
        java.util.List<User> users = listByIds(userIds);
        for (User u : users) {
            if (u.getDeleted() != 1) {
                result.put(u.getId(), java.util.Map.of(
                    "username", u.getUsername(),
                    "nickname", u.getNickname() != null ? u.getNickname() : u.getUsername()));
            }
        }
        return result;
    }
}
