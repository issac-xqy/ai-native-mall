package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.java_ai.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户地址Mapper
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    /**
     * 取消用户所有地址的默认标记
     */
    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefaultByUserId(@Param("userId") Long userId);
}
