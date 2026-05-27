package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.Order;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
