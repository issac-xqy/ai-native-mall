package org.example.java_ai.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.entity.Product;
import org.example.java_ai.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public Result<Map<String, Object>> listProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer publishStatus) {

        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) { wrapper.like(Product::getName, keyword); }
        if (publishStatus != null) { wrapper.eq(Product::getPublishStatus, publishStatus); }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> result = productService.page(page, wrapper);
        return Result.success(Map.of(
            "data", result.getRecords(), "total", result.getTotal(),
            "pageNum", pageNum, "pageSize", pageSize));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) return Result.error("商品不存在");
        productService.incrementClickCount(id);
        return Result.success(Map.of("data", product));
    }

    @PutMapping("/{id}/publish")
    public Result<Map<String, Object>> publishProduct(@PathVariable Long id) {
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>().eq(Product::getId, id)
                .set(Product::getPublishStatus, 1).set(Product::getStatus, 1));
        return success ? Result.success(Map.of("message", "上架成功"))
                : Result.error("上架失败");
    }

    @PutMapping("/{id}/unpublish")
    public Result<Map<String, Object>> unpublishProduct(@PathVariable Long id) {
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>().eq(Product::getId, id)
                .set(Product::getPublishStatus, 2).set(Product::getStatus, 0));
        return success ? Result.success(Map.of("message", "下架成功"))
                : Result.error("下架失败");
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        boolean success = productService.updateById(product);
        return success ? Result.success(Map.of("message", "更新成功"))
                : Result.error("更新失败");
    }

    @PostMapping
    public Result<Map<String, Object>> createProduct(@RequestBody Product product) {
        if (product.getPublishStatus() == null) product.setPublishStatus(0);
        if (product.getStatus() == null) product.setStatus(0);
        if (product.getViewCount() == null) product.setViewCount(0);
        if (product.getClickCount() == null) product.setClickCount(0);
        boolean success = productService.save(product);
        return success ? Result.success(Map.of("message", "创建成功", "productId", product.getId()))
                : Result.error("创建失败");
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        boolean success = productService.removeById(id);
        return success ? Result.success(Map.of("message", "删除成功"))
                : Result.error("删除失败");
    }

    @PutMapping("/batch/publish")
    public Result<Map<String, Object>> batchPublish(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> ids = (java.util.List<Long>) request.get("ids");
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>().in(Product::getId, ids)
                .set(Product::getPublishStatus, 1).set(Product::getStatus, 1));
        return success ? Result.success(Map.of("message", "批量上架成功"))
                : Result.error("批量上架失败");
    }

    @PutMapping("/batch/unpublish")
    public Result<Map<String, Object>> batchUnpublish(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> ids = (java.util.List<Long>) request.get("ids");
        boolean success = productService.update(
            new LambdaUpdateWrapper<Product>().in(Product::getId, ids)
                .set(Product::getPublishStatus, 2).set(Product::getStatus, 0));
        return success ? Result.success(Map.of("message", "批量下架成功"))
                : Result.error("批量下架失败");
    }
}
