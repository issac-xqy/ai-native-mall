package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 商品服务接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 分页查询商品
     */
    Page<Product> listProducts(Integer pageNum, Integer pageSize, Long categoryId, String keyword, String sortField, String sortOrder);

    /**
     * 根据ID查询商品详情
     */
    Product getProductById(Long id);

    /**
     * 创建商品
     */
    Product createProduct(Product product);

    /**
     * 更新商品
     */
    Product updateProduct(Product product);

    /**
     * 删除商品
     */
    boolean deleteProduct(Long id);

    /**
     * AI生成SEO标题
     */
    String generateSeoTitle(Long productId);

    /**
     * AI生成商品描述
     */
    String generateAiDescription(Long productId);

    /**
     * 批量AI分析评论情感
     */
    Map<String, Object> analyzeCommentsSentiment(Long productId);

    /**
     * 智能商品情感分析 - 通过商品名称自动获取评论并分析
     * 
     * @param productName 商品名称（支持模糊匹配）
     * @return 分析报告（包含商品信息、评论数量、满意度、标签等）
     */
    Map<String, Object> analyzeProductSentimentByName(String productName);

    /**
     * 增加商品浏览量
     */
    boolean incrementViewCount(Long productId);

    /**
     * 增加商品点击量
     */
    boolean incrementClickCount(Long productId);
    
    /**
     * 批量查询商品（根据ID列表）
     * 
     * @param productIds 商品ID列表
     * @return 商品列表
     */
    List<Product> getProductsByIds(List<Long> productIds);

    /**
     * 获取销量Top N商品（首页推荐）
     * 只返回已上架且有库存的商品
     * 
     * @param limit 返回数量，默认10
     * @return 商品列表（按销量降序）
     */
    List<Product> getTopSalesProducts(Integer limit);

    /**
     * 获取好评Top N商品（首页推荐）
     * 规则：
     * 1. 评分来源：商品评论表 (product_comment) 中 rating 字段的平均值（保留1位小数）
     * 2. 过滤条件：仅展示已上架 (publish_status=1) 且有库存 (stock>0) 的商品
     * 3. 无评论商品：不参与排名
     * 
     * @param limit 返回数量，默认10
     * @return 商品列表（按平均评分降序）
     */
    List<Product> getTopRatedProducts(Integer limit);
}
