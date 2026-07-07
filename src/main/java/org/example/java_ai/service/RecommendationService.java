package org.example.java_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI 推荐算法服务 — 封装商品推荐/榜单查询 SQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final JdbcTemplate jdbcTemplate;

    /** 个性化推荐 — 基于用户浏览/购买历史 */
    public List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int limit) {
        String sql = """
            SELECT p.id, p.name, p.price, p.original_price, p.image, p.sales,
                   COALESCE(v.count, 0) AS view_weight
            FROM product p
            LEFT JOIN (SELECT product_id, COUNT(*) AS count FROM product_view_log
                       WHERE user_id = ? AND deleted = 0 GROUP BY product_id) v
            ON p.id = v.product_id
            WHERE p.publish_status = 1 AND p.stock > 0 AND p.deleted = 0
            ORDER BY view_weight DESC, p.sales DESC LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, userId, limit);
    }

    /** 热门推荐 — 按销量排行 */
    public List<Map<String, Object>> getHotRecommendations(int limit) {
        String sql = """
            SELECT id, name, price, original_price, image, sales
            FROM product WHERE publish_status = 1 AND stock > 0 AND deleted = 0
            ORDER BY sales DESC LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, limit);
    }
}
