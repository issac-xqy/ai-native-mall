package org.example.java_ai.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.Product;
import org.example.java_ai.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表（后台管理）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer publishStatus) {
        
        log.info("📊 后台查询商品列表 - 页码: {}, 关键词: {}, 发布状态: {}", pageNum, keyword, publishStatus);
        
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }
        
        if (publishStatus != null) {
            wrapper.eq(Product::getPublishStatus, publishStatus);
        }
        
        wrapper.orderByDesc(Product::getCreateTime);
        
        Page<Product> result = productService.page(page, wrapper);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", result.getRecords(),
            "total", result.getTotal(),
            "pageNum", pageNum,
            "pageSize", pageSize
        ));
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "商品不存在"));
        }
        
        // 增加点击量
        productService.incrementClickCount(id);
        
        return ResponseEntity.ok(Map.of("success", true, "data", product));
    }

    /**
     * 上架商品
     */
    @PutMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishProduct(@PathVariable Long id) {
        log.info("📤 上架商品: {}", id);
        
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getPublishStatus, 1)
                .set(Product::getStatus, 1)
        );
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "上架成功" : "上架失败"
        ));
    }

    /**
     * 下架商品
     */
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<Map<String, Object>> unpublishProduct(@PathVariable Long id) {
        log.info("📥 下架商品: {}", id);
        
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getPublishStatus, 2)
                .set(Product::getStatus, 0)
        );
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "下架成功" : "下架失败"
        ));
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        
        log.info("✏️ 更新商品: {}", id);
        product.setId(id);
        
        boolean success = productService.updateById(product);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "更新成功" : "更新失败"
        ));
    }

    /**
     * 创建新商品
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        log.info("➕ 创建商品: {}", product.getName());
        
        // 默认状态
        if (product.getPublishStatus() == null) {
            product.setPublishStatus(0); // 草稿
        }
        if (product.getStatus() == null) {
            product.setStatus(0); // 未上架
        }
        if (product.getViewCount() == null) {
            product.setViewCount(0);
        }
        if (product.getClickCount() == null) {
            product.setClickCount(0);
        }
        
        boolean success = productService.save(product);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "创建成功" : "创建失败",
            "productId", product.getId()
        ));
    }

    /**
     * 删除商品（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        log.info("🗑️ 删除商品: {}", id);
        
        boolean success = productService.removeById(id);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "删除成功" : "删除失败"
        ));
    }

    /**
     * 批量上架
     */
    @PutMapping("/batch/publish")
    public ResponseEntity<Map<String, Object>> batchPublish(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> ids = (java.util.List<Long>) request.get("ids");
        
        log.info("📤 批量上架商品: {} 个", ids.size());
        
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>()
                .in(Product::getId, ids)
                .set(Product::getPublishStatus, 1)
                .set(Product::getStatus, 1)
        );
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "批量上架成功" : "批量上架失败"
        ));
    }

    /**
     * 批量下架
     */
    @PutMapping("/batch/unpublish")
    public ResponseEntity<Map<String, Object>> batchUnpublish(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> ids = (java.util.List<Long>) request.get("ids");
        
        log.info("📥 批量下架商品: {} 个", ids.size());
        
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>()
                .in(Product::getId, ids)
                .set(Product::getPublishStatus, 2)
                .set(Product::getStatus, 0)
        );
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "批量下架成功" : "批量下架失败"
        ));
    }
}
