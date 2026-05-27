package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务接口
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 获取用户地址列表
     */
    List<UserAddress> listByUserId(Long userId);

    /**
     * 添加地址
     */
    UserAddress addAddress(UserAddress address);

    /**
     * 更新地址
     */
    UserAddress updateAddress(UserAddress address);

    /**
     * 设置默认地址
     */
    boolean setDefault(Long id, Long userId);

    /**
     * 删除地址
     */
    boolean deleteAddress(Long id, Long userId);
}
