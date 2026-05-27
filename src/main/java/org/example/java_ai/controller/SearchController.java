package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.entity.Product;
import org.example.java_ai.service.ProductService;
import org.example.java_ai.service.ai.ProductSemanticSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品搜索控制器 - 支持语义搜索
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final ProductSemanticSearchService semanticSearchService;
    private final ProductService productService;
    
    /**
     * 语义搜索商品
     * 
     * @param query 自然语言query（如"适合送女友的生日礼物"）
     * @param limit 返回数量
     * @return 商品列表
     */
    @GetMapping("/semantic")
    public Result<List<Product>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("收到语义搜索请求: query={}, limit={}", query, limit);
        
        // 1. 语义检索商品ID
        List<Long> productIds = semanticSearchService.semanticSearch(query, limit);
        
        if (productIds.isEmpty()) {
            return Result.success(List.of());
        }
        
        // 2. 批量查询商品详情
        List<Product> products = productService.getProductsByIds(productIds);
        
        return Result.success(products);
    }
    
    /**
     * 混合搜索：语义 + 关键词
     * 
     * @param query 语义query
     * @param keyword 关键词
     * @param limit 返回数量
     * @return 商品列表
     */
    @GetMapping("/hybrid")
    public Result<List<Product>> hybridSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("收到混合搜索请求: query={}, keyword={}, limit={}", query, keyword, limit);
        
        // 1. 混合检索商品ID
        List<Long> productIds = semanticSearchService.hybridSearch(query, keyword, limit);
        
        if (productIds.isEmpty()) {
            return Result.success(List.of());
        }
        
        // 2. 批量查询商品详情
        List<Product> products = productService.getProductsByIds(productIds);
        
        return Result.success(products);
    }
    
    /**
     * 重建商品向量索引（管理员接口）
     */
    @PostMapping("/rebuild-index")
    public Result<Map<String, Object>> rebuildIndex() {
        log.info("收到重建索引请求");
        try {
            List<Product> allProducts = productService.list();
            semanticSearchService.batchIndexProducts(allProducts);
            return Result.success(Map.of(
                "message", "索引重建任务已启动",
                "totalProducts", allProducts.size()
            ));
        } catch (Exception e) {
            log.error("重建索引失败", e);
            return Result.error("索引重建失败: " + e.getMessage());
        }
    }
}
