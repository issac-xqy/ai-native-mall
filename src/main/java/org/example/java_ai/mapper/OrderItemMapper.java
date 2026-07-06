package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.java_ai.entity.OrderItem;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId} AND deleted = 0")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /** 批量查询订单商品（修复 N+1 查询） */
    @Select("<script>SELECT * FROM order_item WHERE deleted = 0 AND order_id IN "
            + "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<OrderItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
