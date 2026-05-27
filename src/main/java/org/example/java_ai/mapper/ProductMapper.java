package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.java_ai.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 商品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    /**
     * 根据关键词搜索商品（SQL LIKE）
     * 
     * @param keyword 关键词
     * @param limit 限制数量
     * @return 商品列表
     */
    @Select("SELECT * FROM product WHERE (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) AND status = 1 ORDER BY sales DESC LIMIT #{limit}")
    List<Product> selectByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}
