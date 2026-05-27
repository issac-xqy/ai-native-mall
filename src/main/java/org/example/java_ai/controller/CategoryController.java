package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.entity.ProductCategory;
import org.example.java_ai.service.ProductCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器（前台）
 */
@Slf4j
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryService categoryService;

    /**
     * 获取分类树（仅返回启用的分类）
     */
    @GetMapping("/tree")
    public Result<List<ProductCategory>> getCategoryTree() {
        log.info("📂 获取分类树");
        
        List<ProductCategory> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 根据父ID获取子分类
     */
    @GetMapping("/children/{parentId}")
    public Result<List<ProductCategory>> getChildren(@PathVariable Long parentId) {
        log.info("📂 获取子分类 - parentId: {}", parentId);
        
        List<ProductCategory> children = categoryService.getChildrenByParentId(parentId);
        return Result.success(children);
    }

    /**
     * 获取所有分类（不分层级）
     */
    @GetMapping("/list")
    public Result<List<ProductCategory>> getAllCategories() {
        log.info("📂 获取所有分类");
        
        List<ProductCategory> categories = categoryService.list();
        return Result.success(categories);
    }
}
