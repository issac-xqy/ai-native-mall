package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.dto.LoginResult;
import org.example.java_ai.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 用户注册
     */
    User register(String username, String password, String nickname, String phone, String email);

    /**
     * 用户登录 — 返回双 token
     */
    LoginResult login(String username, String password);

    /**
     * 刷新 token — 用 refresh token 换新的 access + refresh token
     */
    LoginResult refreshToken(String refreshToken);

    /**
     * 更新用户信息
     */
    User updateUserInfo(Long userId, User user);

    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 批量查询用户信息 Map (userId → {username, nickname})
     */
    java.util.Map<Long, java.util.Map<String, Object>> getUserMap(java.util.Collection<Long> userIds);
}
