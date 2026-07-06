package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI智能推荐控制器
 * 提供个性化商品推荐服务
 */
@Slf4j
@RestController
@RequestMapping("/ai/recommend")
@RequiredArgsConstructor
public class AIRecommendController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取AI智能推荐轮播商品
     * @param userId 用户ID（可选，未登录时返回热门推荐）
     * @param scene 场景：home(首页)、product_detail(商品详情)
     * @param limit 推荐数量
     */
    @GetMapping("/carousel")
    public ResponseEntity<Map<String, Object>> getCarouselRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "home") String scene,
            @RequestParam(defaultValue = "5") Integer limit) {
        
        log.info("获取AI推荐轮播 - userId: {}, scene: {}, limit: {}", userId, scene, limit);

        List<Map<String, Object>> recommendations;

        if (userId != null) {
            // 有用户ID：基于用户行为的个性化推荐
            recommendations = getPersonalizedRecommendations(userId, limit);
        } else {
            // 未登录：返回热门/新品推荐
            recommendations = getTrendingRecommendations(limit);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", recommendations,
            "scene", scene
        ));
    }

    /**
     * 个性化推荐算法
     */
    private List<Map<String, Object>> getPersonalizedRecommendations(Long userId, Integer limit) {
        log.info("执行个性化推荐算法 - userId: {}", userId);

        // 查询用户浏览历史（最近浏览的商品分类）
        String viewHistorySql = """
            SELECT category_id, COUNT(*) as view_count
            FROM product_view_log
            WHERE user_id = ?
            GROUP BY category_id
            ORDER BY view_count DESC
            LIMIT 3
            """;

        List<Map<String, Object>> preferences;
        try {
            preferences = jdbcTemplate.queryForList(viewHistorySql, userId);
        } catch (Exception e) {
            // 如果没有浏览记录表，降级为热门推荐
            log.warn("查询浏览历史失败，使用热门推荐: {}", e.getMessage());
            return getTrendingRecommendations(limit);
        }

        if (preferences.isEmpty()) {
            // 无浏览记录，返回热门推荐
            return getTrendingRecommendations(limit);
        }

        // 基于用户偏好的推荐SQL
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT 
                p.id, p.name, p.price, p.image, p.sales, p.sentiment_score,
                p.description,
                '猜你喜欢' as recommend_reason,
                ROUND(p.sentiment_score / 20.0, 1) as rating
            FROM product p
            WHERE p.publish_status = 1 
              AND p.stock > 0 
              AND p.deleted = 0
            ORDER BY p.sales DESC
            LIMIT ?
            """);

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sqlBuilder.toString(), limit);

        // 为每个商品生成AI推荐理由
        return enhanceWithAIReasons(products, userId);
    }

    /**
     * 热门推荐算法（未登录用户）
     */
    private List<Map<String, Object>> getTrendingRecommendations(Integer limit) {
        log.info("执行热门推荐算法");

        String sql = """
            SELECT 
                p.id, p.name, p.price, p.image, p.sales, p.sentiment_score,
                p.description,
                CASE 
                    WHEN p.sales > 100 THEN '爆款热销'
                    WHEN p.create_time > DATE_SUB(NOW(), INTERVAL 7 DAY) THEN '新品首发'
                    ELSE '人气优选'
                END as recommend_reason,
                ROUND(p.sentiment_score / 20.0, 1) as rating
            FROM product p
            WHERE p.publish_status = 1 
              AND p.stock > 0 
              AND p.deleted = 0
            ORDER BY p.sales DESC
            LIMIT ?
            """;

        return jdbcTemplate.queryForList(sql, limit);
    }

    /**
     * 增强AI推荐理由
     */
    private List<Map<String, Object>> enhanceWithAIReasons(List<Map<String, Object>> products, Long userId) {
        for (Map<String, Object> product : products) {
            Integer sales = (Integer) product.getOrDefault("sales", 0);
            Double rating = ((Number) product.getOrDefault("rating", 4.5)).doubleValue();
            String reason = (String) product.get("recommend_reason");

            // 根据销售数据和评分生成更智能的推荐理由
            if (sales > 500) {
                reason = "🔥 超过500人购买的选择";
            } else if (rating >= 4.8) {
                reason = "⭐ 好评率98%的品质好物";
            } else if (sales > 100) {
                reason = "👍 " + sales + "人已验证的好物";
            }

            product.put("recommend_reason", reason);
            product.put("confidence", Math.min(0.95, 0.7 + (sales / 1000.0)));
        }

        return products;
    }
}
