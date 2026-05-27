package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.java_ai.entity.Order;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND deleted = 0")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /** 原子更新订单状态 */
    @Update("UPDATE orders SET status = #{newStatus} WHERE order_no = #{orderNo} AND status = #{expectedStatus}")
    int updateStatus(@Param("orderNo") String orderNo, @Param("expectedStatus") int expectedStatus,
                     @Param("newStatus") int newStatus);

    /** 原子扣减库存 */
    @Update("UPDATE product SET sales = sales + #{quantity}, stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /** 恢复库存 */
    @Update("UPDATE product SET stock = stock + #{quantity}, sales = sales - #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
