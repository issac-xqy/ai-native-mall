package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.java_ai.entity.ProductComment;
import java.util.List;

/**
 * 商品评论 Mapper
 */
@Mapper
public interface ProductCommentMapper extends BaseMapper<ProductComment> {

    /**
     * 根据商品ID查询评论列表（分页）
     */
    @Select("SELECT id, product_id, user_id, content, sentiment, ai_tags, summary, rating, create_time FROM product_comment WHERE product_id = #{productId} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<ProductComment> selectByProductId(@Param("productId") Long productId,
                                           @Param("offset") Integer offset,
                                           @Param("pageSize") Integer pageSize);

    /**
     * 统计商品评论数量
     */
    @Select("SELECT COUNT(*) FROM product_comment WHERE product_id = #{productId} AND (deleted = 0 OR deleted IS NULL)")
    Long countByProductId(@Param("productId") Long productId);

    /**
     * 插入评论
     */
    @Insert("INSERT INTO product_comment (product_id, user_id, content, sentiment, ai_tags, summary, rating, deleted, create_time, update_time) VALUES (#{productId}, #{userId}, #{content}, #{sentiment}, #{aiTags}, #{summary}, #{rating}, 0, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductComment comment);

    /**
     * 逻辑删除评论
     */
    @Update("UPDATE product_comment SET deleted = 1 WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
