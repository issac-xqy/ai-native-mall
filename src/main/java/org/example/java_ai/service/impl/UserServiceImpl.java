package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.dto.LoginResult;
import org.example.java_ai.entity.User;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.mapper.UserMapper;
import org.example.java_ai.service.UserService;
import org.example.java_ai.util.TokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;

    private final ConcurrentHashMap<String, Integer> loginFailCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockedUntil = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_FAILS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

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
        Long lockExpire = lockedUntil.get(username);
        if (lockExpire != null && System.currentTimeMillis() < lockExpire) {
            long minutesLeft = (lockExpire - System.currentTimeMillis()) / 60000 + 1;
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "账号已锁定，请" + minutesLeft + "分钟后再试");
        }

        User user = getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            int fails = loginFailCount.merge(username, 1, Integer::sum);
            if (fails >= MAX_LOGIN_FAILS) {
                lockedUntil.put(username, System.currentTimeMillis() + LOCK_DURATION_MS);
                loginFailCount.remove(username);
                log.warn("账号 {} 登录失败次数过多，已锁定15分钟", username);
                throw new BusinessException(ResultCode.BAD_REQUEST, "登录失败次数过多，账号已锁定15分钟");
            }
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号已被禁用");
        }

        loginFailCount.remove(username);
        lockedUntil.remove(username);
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
        // 旋转 refresh token：发放全新的一对
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
}
