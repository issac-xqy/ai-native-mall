package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.User;
import org.example.java_ai.mapper.UserMapper;
import org.example.java_ai.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User getUserByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User register(String username, String password, String nickname, String phone, String email) {
        // 检查用户名是否已存在
        User existingUser = getUserByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1);

        save(user);
        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public String login(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        log.info("用户登录成功: {}", username);
        // TODO: 生成JWT Token
        return "mock-jwt-token-" + user.getId();
    }

    @Override
    public User updateUserInfo(Long userId, User user) {
        User existingUser = getById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 只更新允许修改的字段
        if (StringUtils.hasText(user.getNickname())) {
            existingUser.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getPhone())) {
            existingUser.setPhone(user.getPhone());
        }
        if (StringUtils.hasText(user.getEmail())) {
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
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        log.info("用户密码修改成功: {}", userId);
        return true;
    }
}
