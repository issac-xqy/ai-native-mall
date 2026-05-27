package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.Product;
import org.example.java_ai.entity.ProductComment;
import org.example.java_ai.mapper.ProductCommentMapper;
import org.example.java_ai.mapper.ProductMapper;
import org.example.java_ai.service.ProductService;
import org.example.java_ai.service.ai.ProductOperationService;
import org.example.java_ai.service.ai.ProductSemanticSearchService;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductOperationService productOperationService;
    private final ProductCommentMapper productCommentMapper;
    private final ProductSemanticSearchService semanticSearchService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<Product> listProducts(Integer pageNum, Integer pageSize, Long categoryId, String keyword, String sortField, String sortOrder) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        // 用户端只显示已上架且有库存的商品
        wrapper.eq(Product::getPublishStatus, 1)
               .gt(Product::getStock, 0)
               .eq(Product::getDeleted, 0);
        
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                               .or()
                               .like(Product::getDescription, keyword));
        }
        
        // 动态排序
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortField) {
            case "sales":
                wrapper.orderBy(true, isAsc, Product::getSales);
                break;
            case "price":
                wrapper.orderBy(true, isAsc, Product::getPrice);
                break;
            default:
                wrapper.orderByDesc(Product::getCreateTime);
        }
        
        return page(page, wrapper);
    }

    @Override
    public Product getProductById(Long id) {
        // 查询商品（包括未上架的，前端根据publishStatus显示无货）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getId, id)
               .eq(Product::getDeleted, 0);
        
        Product product = getOne(wrapper);
        if (product != null) {
            // 异步增加点击量（进入详情页才算点击），不影响响应速度
            CompletableFuture.runAsync(() -> {
                incrementClickCount(id);
            });
        }
        return product;
    }

    @Override
    public Product createProduct(Product product) {
        save(product);
        log.info("创建商品成功: {}", product.getName());
        
        // 异步索引到向量库
        semanticSearchService.indexProduct(product);
        
        return product;
    }

    @Override
    public Product updateProduct(Product product) {
        updateById(product);
        log.info("更新商品成功: {}", product.getName());
        
        // 异步更新向量索引
        semanticSearchService.indexProduct(product);
        
        return product;
    }

    @Override
    public boolean deleteProduct(Long id) {
        // 异步删除向量索引
        semanticSearchService.removeProductIndex(id);
        
        return removeById(id);
    }

    @Override
    public String generateSeoTitle(Long productId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        try {
            CompletableFuture<String> future = productOperationService.generateSeoTitle(
                product.getName(),
                "商品分类", // TODO: 从分类表获取
                product.getSpecs() != null ? product.getSpecs() : ""
            );
            String seoTitle = future.get();
            
            // 更新商品SEO标题
            product.setSeoTitle(seoTitle);
            updateById(product);
            
            log.info("AI生成SEO标题成功: {}", seoTitle);
            return seoTitle;
        } catch (Exception e) {
            log.error("生成SEO标题失败", e);
            throw new RuntimeException("生成SEO标题失败: " + e.getMessage());
        }
    }

    @Override
    public String generateAiDescription(Long productId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        try {
            // 解析规格参数
            Map<String, String> specs = Map.of(); // TODO: 从JSON解析
            
            CompletableFuture<String> future = productOperationService.generateProductDescription(
                product.getName(),
                specs,
                "目标用户" // TODO: 从商品分类推断
            );
            String description = future.get();
            
            // 更新商品AI描述
            product.setAiDescription(description);
            updateById(product);
            
            log.info("AI生成商品描述成功，长度: {}", description.length());
            return description;
        } catch (Exception e) {
            log.error("生成商品描述失败", e);
            throw new RuntimeException("生成商品描述失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> analyzeCommentsSentiment(Long productId) {
        // 查询该商品的所有评论
        List<ProductComment> comments = productCommentMapper.selectList(new LambdaQueryWrapper<ProductComment>()
            .eq(ProductComment::getProductId, productId));
        
        if (comments.isEmpty()) {
            return Map.of("total_comments", 0, "report", "暂无评论");
        }

        List<String> commentTexts = comments.stream()
            .map(ProductComment::getContent)
            .collect(Collectors.toList());

        try {
            CompletableFuture<Map<String, Object>> future = 
                productOperationService.generateReputationReport(commentTexts);
            return future.get();
        } catch (Exception e) {
            log.error("批量分析评论情感失败", e);
            return Map.of("error", "分析失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> analyzeProductSentimentByName(String productName) {
        log.info("智能商品情感分析 - 搜索商品: {}", productName);
        
        // 1. 根据商品名称模糊搜索商品（注意：使用 and 包裹 or 条件，避免 SQL 优先级问题）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(Product::getName, productName)
                          .or()
                          .like(Product::getDescription, productName))
               .eq(Product::getStatus, 1)
               .last("LIMIT 1");
        
        Product product = getOne(wrapper);
        if (product == null) {
            log.warn("未找到商品: {}", productName);
            return Map.of(
                "success", false,
                "message", "未找到商品：" + productName,
                "suggestions", "请检查商品名称是否正确"
            );
        }
        
        log.info("找到商品: ID={}, 名称={}, 状态={}", product.getId(), product.getName(), product.getStatus());
        
        // 查询数据库中该商品ID的所有评论（不过滤deleted）
        List<ProductComment> allComments = productCommentMapper.selectList(
            new LambdaQueryWrapper<ProductComment>()
                .eq(ProductComment::getProductId, product.getId()));
        log.info("🔍 原始查询: 商品ID={}, 总评论数={}", product.getId(), allComments.size());
        if (!allComments.isEmpty()) {
            allComments.forEach(c -> log.info("   评论ID={}, deleted={}, content={}", c.getId(), c.getDeleted(), c.getContent()));
        }
        
        // 2. 查询该商品的所有评论（过滤已删除的，兼容 NULL 值）
        List<ProductComment> comments = productCommentMapper.selectList(
            new LambdaQueryWrapper<ProductComment>()
                .eq(ProductComment::getProductId, product.getId())
                .and(w -> w.eq(ProductComment::getDeleted, 0).or().isNull(ProductComment::getDeleted)));
        
        log.info("查询到评论数量: {} (商品ID: {})", comments.size(), product.getId());
        
        if (comments.isEmpty()) {
            log.info("商品暂无评论: {}", product.getName());
            return Map.of(
                "success", true,
                "product", Map.of(
                    "id", product.getId(),
                    "name", product.getName(),
                    "price", product.getPrice()
                ),
                "total_comments", 0,
                "message", "该商品暂无评论，快去引导用户评价吧！"
            );
        }
        
        log.info("找到 {} 条评论，开始AI分析...", comments.size());
        
        // 3. 提取评论文本
        List<String> commentTexts = comments.stream()
            .map(ProductComment::getContent)
            .collect(Collectors.toList());
        
        // 4. 调用AI生成口碑报告
        try {
            CompletableFuture<Map<String, Object>> future = 
                productOperationService.generateReputationReport(commentTexts);
            Map<String, Object> report = future.get();
            
            // 5. 组合返回结果
            Map<String, Object> result = new java.util.HashMap<>(report);
            result.put("success", true);
            
            // 使用 HashMap 避免 product.getImage() 为 null 导致 Map.of 抛出 NPE
            Map<String, Object> productInfo = new java.util.HashMap<>();
            productInfo.put("id", product.getId());
            productInfo.put("name", product.getName());
            productInfo.put("price", product.getPrice());
            productInfo.put("image", product.getImage());
            
            result.put("product", productInfo);
            result.put("total_comments", comments.size());
            
            log.info("智能商品情感分析完成");
            return result;
        } catch (Exception e) {
            log.error("智能商品情感分析失败", e);
            
            Map<String, Object> errorProductInfo = new java.util.HashMap<>();
            errorProductInfo.put("id", product.getId());
            errorProductInfo.put("name", product.getName());
            
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "AI分析失败: " + e.getMessage());
            errorResult.put("product", errorProductInfo);
            errorResult.put("total_comments", comments.size());
            return errorResult;
        }
    }

    @Override
    public boolean incrementViewCount(Long productId) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .setSql("view_count = view_count + 1"));
    }

    @Override
    public boolean incrementClickCount(Long productId) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .setSql("click_count = click_count + 1"));
    }
    
    @Override
    public List<Product> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getId, productIds)
               .eq(Product::getStatus, 1);
        
        return list(wrapper);
    }

    @Override
    public List<Product> getTopSalesProducts(Integer limit) {
        // 查询销量Top N商品，用户端只显示已上架且有库存的
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getPublishStatus, 1)
               .gt(Product::getStock, 0)
               .eq(Product::getDeleted, 0)
               .orderByDesc(Product::getSales)
               .last("LIMIT " + limit);
        
        List<Product> products = list(wrapper);
        log.info("获取销量Top{}商品，数量: {}", limit, products.size());
        return products;
    }

    @Override
    public List<Product> getTopRatedProducts(Integer limit) {
        // 基于评论表的rating字段计算平均值进行排名，无评论商品不参与排名
        String sql = """
            SELECT p.id, p.name, p.price, p.original_price, p.image,
                   ROUND(AVG(c.rating), 1) as avg_rating
            FROM product p
            INNER JOIN product_comment c ON p.id = c.product_id
            WHERE p.publish_status = 1 
              AND p.stock > 0 
              AND p.deleted = 0
              AND c.deleted = 0
            GROUP BY p.id, p.name, p.price, p.original_price, p.image
            ORDER BY avg_rating DESC
            LIMIT ?
            """;
            
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit);
        List<Product> products = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Product p = new Product();
            p.setId(((Number) row.get("id")).longValue());
            p.setName((String) row.get("name"));
            p.setPrice((BigDecimal) row.get("price"));
            p.setOriginalPrice(row.get("original_price") != null ? (BigDecimal) row.get("original_price") : null);
            p.setImage((String) row.get("image"));
            // 将计算出的平均评分赋值给sentimentScore供前端显示
            Object avgRating = row.get("avg_rating");
            if (avgRating != null) {
                p.setSentimentScore(new BigDecimal(avgRating.toString()));
            }
            products.add(p);
        }
        log.info("获取好评Top{}商品，数量: {}", limit, products.size());
        return products;
    }
}
