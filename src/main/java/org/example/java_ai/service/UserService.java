package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
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
     * 用户登录
     */
    String login(String username, String password);

    /**
     * 更新用户信息
     */
    User updateUserInfo(Long userId, User user);

    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
}
