package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
