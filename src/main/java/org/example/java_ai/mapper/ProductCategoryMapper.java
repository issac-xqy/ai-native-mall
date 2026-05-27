package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.ProductCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 商品分类Mapper
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
