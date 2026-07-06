package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.java_ai.entity.ShoppingCart;

import java.util.List;
import java.util.Map;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    @Select("""
        SELECT sc.id, sc.quantity, sc.product_id as productId,
               p.name as productName, p.price, p.image, p.stock
        FROM shopping_cart sc
        JOIN product p ON sc.product_id = p.id
        WHERE sc.user_id = #{userId} AND sc.deleted = 0
        ORDER BY sc.create_time DESC
        """)
    List<Map<String, Object>> selectCartWithProduct(@Param("userId") Long userId);
}
