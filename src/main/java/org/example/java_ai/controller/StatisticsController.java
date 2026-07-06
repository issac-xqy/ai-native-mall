package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取Dashboard统计数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        try {
            // 今日销售额（已支付订单）
            BigDecimal todaySales = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 1 AND DATE(create_time) = CURDATE()",
                BigDecimal.class
            );
            stats.put("todaySales", todaySales != null ? todaySales : BigDecimal.ZERO);
            
            // 今日订单数
            Integer todayOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE DATE(create_time) = CURDATE()",
                Integer.class
            );
            stats.put("todayOrders", todayOrders != null ? todayOrders : 0);
            
            // AI对话次数（从user表或订单备注统计）
            Long aiConversations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE remark LIKE '%AI%'",
                Long.class
            );
            stats.put("aiConversations", aiConversations != null ? aiConversations : 0);
            
            // 用户满意度（商品情感评分平均值）
            BigDecimal satisfaction = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(sentiment_score), 0) FROM product WHERE sentiment_score > 0",
                BigDecimal.class
            );
            stats.put("satisfaction", satisfaction != null ? satisfaction : BigDecimal.ZERO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "获取统计数据失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取近7天销售趋势
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<Map<String, Object>> getSalesTrend() {
        try {
            String sql = """
                SELECT 
                    DATE(create_time) as date,
                    COALESCE(SUM(total_amount), 0) as amount,
                    COUNT(*) as orders
                FROM orders 
                WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                GROUP BY DATE(create_time)
                ORDER BY date ASC
                """;
            
            List<Map<String, Object>> trendData = jdbcTemplate.queryForList(sql);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", trendData
            ));
            
        } catch (Exception e) {
            log.error("获取销售趋势失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "获取销售趋势失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取商品销量排行
     */
    @GetMapping("/product-ranking")
    public ResponseEntity<Map<String, Object>> getProductRanking() {
        try {
            String sql = """
                SELECT 
                    p.id,
                    p.name,
                    p.sales,
                    p.price
                FROM product p
                WHERE p.status = 1 AND p.deleted = 0
                ORDER BY p.sales DESC
                LIMIT 10
                """;
            
            List<Map<String, Object>> ranking = jdbcTemplate.queryForList(sql);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ranking
            ));
            
        } catch (Exception e) {
            log.error("获取商品排行失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "获取商品排行失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取商品分类统计
     */
    @GetMapping("/category-stats")
    public ResponseEntity<Map<String, Object>> getCategoryStats() {
        try {
            String sql = """
                SELECT 
                    c.name as category,
                    COUNT(p.id) as product_count,
                    COALESCE(SUM(p.sales), 0) as total_sales
                FROM product_category c
                LEFT JOIN product p ON c.id = p.category_id AND p.deleted = 0
                GROUP BY c.id, c.name
                ORDER BY total_sales DESC
                """;
            
            List<Map<String, Object>> categoryStats = jdbcTemplate.queryForList(sql);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", categoryStats
            ));
            
        } catch (Exception e) {
            log.error("获取分类统计失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "获取分类统计失败: " + e.getMessage()
            ));
        }
    }
}
