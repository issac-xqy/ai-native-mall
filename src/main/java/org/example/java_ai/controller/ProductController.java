package org.example.java_ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.PageResult;
import org.example.java_ai.common.Result;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.entity.Product;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public Result<PageResult<Product>> listProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        Page<Product> page = productService.listProducts(pageNum, pageSize, categoryId, keyword, sortField, sortOrder);
        PageResult<Product> pageResult = PageResult.of(
            page.getRecords(),
            page.getTotal(),
            page.getCurrent(),
            page.getSize()
        );
        
        return Result.success(pageResult);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return Result.success(product);
    }

    /**
     * 创建商品
     */
    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return Result.success("商品创建成功", created);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        product.setId(id);
        Product updated = productService.updateProduct(product);
        return Result.success("商品更新成功", updated);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        boolean success = productService.deleteProduct(id);
        if (!success) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return Result.success("商品删除成功", null);
    }

    /**
     * AI生成SEO标题
     */
    @PostMapping("/{id}/seo-title")
    public Result<Map<String, String>> generateSeoTitle(@PathVariable Long id) {
        try {
            String seoTitle = productService.generateSeoTitle(id);
            return Result.success(Map.of("seoTitle", seoTitle));
        } catch (Exception e) {
            log.error("生成SEO标题失败, productId={}", id, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI生成SEO标题失败: " + e.getMessage());
        }
    }

    /**
     * AI生成商品描述
     */
    @PostMapping("/{id}/ai-description")
    public Result<Map<String, String>> generateAiDescription(@PathVariable Long id) {
        try {
            String description = productService.generateAiDescription(id);
            return Result.success(Map.of("description", description));
        } catch (Exception e) {
            log.error("生成AI描述失败, productId={}", id, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI生成商品描述失败: " + e.getMessage());
        }
    }

    /**
     * 批量AI分析商品评论情感
     */
    @PostMapping("/{id}/analyze-comments")
    public Result<Map<String, Object>> analyzeComments(@PathVariable Long id) {
        try {
            Map<String, Object> report = productService.analyzeCommentsSentiment(id);
            return Result.success(report);
        } catch (Exception e) {
            log.error("分析评论情感失败, productId={}", id, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "评论情感分析失败: " + e.getMessage());
        }
    }

    /**
     * 智能商品情感分析 - 通过商品名称自动获取评论并分析
     */
    @GetMapping("/analyze-by-name")
    public Result<Map<String, Object>> analyzeProductSentimentByName(
            @RequestParam String productName) {
        try {
            log.info("智能商品情感分析 - 商品名称: {}", productName);
            Map<String, Object> analysis = productService.analyzeProductSentimentByName(productName);
            return Result.success(analysis);
        } catch (Exception e) {
            log.error("智能商品情感分析失败, productName={}", productName, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "情感分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取销量Top10商品（首页推荐）
     * 只返回已上架且有库存的商品
     */
    @GetMapping("/top-sales")
    public Result<java.util.List<Product>> getTopSalesProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            java.util.List<Product> products = productService.getTopSalesProducts(limit);
            return Result.success(products);
        } catch (Exception e) {
            log.error("获取销量排行失败", e);
            throw new BusinessException(ResultCode.ERROR, "获取销量排行失败: " + e.getMessage());
        }
    }

    /**
     * 获取好评Top10商品（首页推荐）
     * 只返回已上架且有库存的商品
     */
    @GetMapping("/top-rated")
    public Result<java.util.List<Product>> getTopRatedProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            java.util.List<Product> products = productService.getTopRatedProducts(limit);
            return Result.success(products);
        } catch (Exception e) {
            log.error("获取好评排行失败", e);
            throw new BusinessException(ResultCode.ERROR, "获取好评排行失败: " + e.getMessage());
        }
    }
}
