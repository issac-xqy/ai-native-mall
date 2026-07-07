package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.UserAddress;
import org.example.java_ai.mapper.UserAddressMapper;
import org.example.java_ai.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public List<UserAddress> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress addAddress(UserAddress address) {
        // 如果是第一个地址或标记为默认，则设为默认
        Long count = count(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, address.getUserId()));
        
        if (count == 0 || address.getIsDefault() == 1) {
            baseMapper.clearDefaultByUserId(address.getUserId());
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        
        save(address);
        log.info("添加地址成功: {}", address.getDetailAddress());
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress updateAddress(UserAddress address) {
        // 如果设置为默认地址，先取消其他默认
        if (address.getIsDefault() == 1) {
            baseMapper.clearDefaultByUserId(address.getUserId());
        }
        
        updateById(address);
        log.info("更新地址成功: {}", address.getId());
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id, Long userId) {
        // 验证地址归属
        UserAddress address = getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        
        // 取消所有默认
        baseMapper.clearDefaultByUserId(userId);
        
        // 设置当前为默认
        address.setIsDefault(1);
        updateById(address);
        
        log.info("设置默认地址成功: {}", id);
        return true;
    }

    @Override
    public boolean deleteAddress(Long id, Long userId) {
        UserAddress address = getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        
        boolean isDefault = address.getIsDefault() == 1;
        removeById(id);
        
        // 如果删除的是默认地址，将第一个地址设为默认
        if (isDefault) {
            List<UserAddress> addresses = listByUserId(userId);
            if (!addresses.isEmpty()) {
                addresses.get(0).setIsDefault(1);
                updateById(addresses.get(0));
            }
        }
        
        log.info("删除地址成功: {}", id);
        return true;
    }
}
