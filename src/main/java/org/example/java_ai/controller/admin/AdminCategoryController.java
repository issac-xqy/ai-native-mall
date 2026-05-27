package org.example.java_ai.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.ProductCategory;
import org.example.java_ai.service.ProductCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品分类管理控制器（后台）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final ProductCategoryService categoryService;

    /**
     * 获取所有分类列表（后台管理用，包含禁用的）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listCategories(
            @RequestParam(required = false) Integer status) {
        
        log.info("📊 后台查询分类列表 - 状态: {}", status);
        
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(ProductCategory::getStatus, status);
        }
        
        wrapper.orderByAsc(ProductCategory::getSortOrder)
               .orderByAsc(ProductCategory::getId);
        
        List<ProductCategory> categories = categoryService.list(wrapper);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", categories,
            "total", categories.size()
        ));
    }

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getCategoryTree() {
        log.info("📊 后台获取分类树");
        
        List<ProductCategory> tree = categoryService.getCategoryTree();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", tree
        ));
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody ProductCategory category) {
        log.info("➕ 创建分类: {}", category.getName());
        
        try {
            ProductCategory created = categoryService.createCategory(category);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "创建成功",
                "data", created
            ));
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long id,
            @RequestBody ProductCategory category) {
        
        log.info("✏️ 更新分类: ID={}", id);
        
        category.setId(id);
        
        try {
            ProductCategory updated = categoryService.updateCategory(category);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "更新成功",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        log.info("🗑️ 删除分类: ID={}", id);
        
        try {
            boolean success = categoryService.deleteCategory(id);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "删除成功" : "删除失败"
            ));
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 批量删除分类
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteCategories(@RequestBody List<Long> ids) {
        log.info("🗑️ 批量删除分类: {}", ids);
        
        try {
            boolean success = categoryService.batchDeleteCategories(ids);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "批量删除成功"
            ));
        } catch (Exception e) {
            log.error("批量删除分类失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新分类状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        log.info("🔄 更新分类状态 - ID: {}, Status: {}", id, request.get("status"));
        
        try {
            Integer status = request.get("status");
            boolean success = categoryService.updateStatus(id, status);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "状态更新成功"
            ));
        } catch (Exception e) {
            log.error("更新分类状态失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新分类排序
     */
    @PutMapping("/{id}/sort")
    public ResponseEntity<Map<String, Object>> updateSortOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        log.info("🔄 更新分类排序 - ID: {}, SortOrder: {}", id, request.get("sortOrder"));
        
        try {
            Integer sortOrder = request.get("sortOrder");
            boolean success = categoryService.updateSortOrder(id, sortOrder);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "排序更新成功"
            ));
        } catch (Exception e) {
            log.error("更新分类排序失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
