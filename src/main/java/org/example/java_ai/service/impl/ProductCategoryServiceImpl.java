package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.ProductCategory;
import org.example.java_ai.mapper.ProductCategoryMapper;
import org.example.java_ai.mapper.ProductMapper;
import org.example.java_ai.service.ProductCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {

    private final ProductMapper productMapper;

    @Override
    public List<ProductCategory> getCategoryTree() {
        log.info("获取分类树");
        
        // 查询所有启用的分类
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getStatus, 1)
               .orderByAsc(ProductCategory::getSortOrder)
               .orderByAsc(ProductCategory::getId);
        
        List<ProductCategory> allCategories = list(wrapper);
        
        // 构建树形结构
        return buildTree(allCategories, 0L);
    }

    @Override
    public List<ProductCategory> getChildrenByParentId(Long parentId) {
        log.info("获取子分类 - parentId: {}", parentId);
        
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getParentId, parentId)
               .eq(ProductCategory::getStatus, 1)
               .orderByAsc(ProductCategory::getSortOrder);
        
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategory createCategory(ProductCategory category) {
        log.info("创建分类: {}", category.getName());
        
        // 默认值设置
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        
        save(category);
        log.info("✅ 分类创建成功 - ID: {}", category.getId());
        
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategory updateCategory(ProductCategory category) {
        log.info("更新分类: ID={}, Name={}", category.getId(), category.getName());
        
        // 检查分类是否存在
        ProductCategory existing = getById(category.getId());
        if (existing == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 不允许将分类的父节点设置为自己或其子节点（防止循环引用）
        if (category.getParentId() != null && category.getParentId().equals(category.getId())) {
            throw new RuntimeException("不能将分类设置为自己的子分类");
        }
        
        updateById(category);
        log.info("✅ 分类更新成功 - ID: {}", category.getId());
        
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long id) {
        log.info("删除分类: ID={}", id);
        
        // 检查是否有子分类
        Long childrenCount = count(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id));
        
        if (childrenCount > 0) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
        
        // 检查是否有关联商品
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<org.example.java_ai.entity.Product>()
                .eq(org.example.java_ai.entity.Product::getCategoryId, id));
        
        if (productCount > 0) {
            throw new RuntimeException("该分类下存在" + productCount + "个商品，无法删除");
        }
        
        boolean success = removeById(id);
        log.info("✅ 分类删除成功 - ID: {}", id);
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteCategories(List<Long> ids) {
        log.info("批量删除分类: {}", ids);
        
        for (Long id : ids) {
            try {
                deleteCategory(id);
            } catch (Exception e) {
                log.warn("分类 {} 删除失败: {}", id, e.getMessage());
                throw new RuntimeException("分类 " + id + " 删除失败: " + e.getMessage());
            }
        }
        
        log.info("✅ 批量删除分类成功");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        log.info("更新分类状态 - ID: {}, Status: {}", id, status);
        
        ProductCategory category = getById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        category.setStatus(status);
        boolean success = updateById(category);
        
        log.info("✅ 分类状态更新成功");
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(Long id, Integer sortOrder) {
        log.info("更新分类排序 - ID: {}, SortOrder: {}", id, sortOrder);
        
        ProductCategory category = getById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        category.setSortOrder(sortOrder);
        boolean success = updateById(category);
        
        log.info("✅ 分类排序更新成功");
        return success;
    }

    /**
     * 递归构建分类树
     */
    private List<ProductCategory> buildTree(List<ProductCategory> allCategories, Long parentId) {
        List<ProductCategory> tree = new ArrayList<>();
        for (ProductCategory category : allCategories) {
            if (category.getParentId().equals(parentId)) {
                List<ProductCategory> children = buildTree(allCategories, category.getId());
                category.setChildren(children.isEmpty() ? null : children);
                tree.add(category);
            }
        }
        return tree;
    }
}
