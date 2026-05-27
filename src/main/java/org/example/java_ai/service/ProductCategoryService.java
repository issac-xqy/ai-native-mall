package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.entity.ProductCategory;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 获取所有启用的分类（树形结构）
     */
    List<ProductCategory> getCategoryTree();

    /**
     * 根据父ID获取子分类列表
     */
    List<ProductCategory> getChildrenByParentId(Long parentId);

    /**
     * 创建分类
     */
    ProductCategory createCategory(ProductCategory category);

    /**
     * 更新分类
     */
    ProductCategory updateCategory(ProductCategory category);

    /**
     * 删除分类（检查是否有子分类或关联商品）
     */
    boolean deleteCategory(Long id);

    /**
     * 批量删除分类
     */
    boolean batchDeleteCategories(List<Long> ids);

    /**
     * 更新分类状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 更新排序
     */
    boolean updateSortOrder(Long id, Integer sortOrder);
}
