package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.java_ai.entity.ProductComment;
import java.util.List;

@Mapper
public interface ProductCommentMapper extends BaseMapper<ProductComment> {

    @Select("SELECT id, product_id, user_id, content, sentiment, ai_tags, summary, rating, create_time FROM product_comment WHERE product_id = #{productId} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<ProductComment> selectByProductId(@Param("productId") Long productId,
                                           @Param("offset") Integer offset,
                                           @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM product_comment WHERE product_id = #{productId} AND (deleted = 0 OR deleted IS NULL)")
    Long countByProductId(@Param("productId") Long productId);
}
