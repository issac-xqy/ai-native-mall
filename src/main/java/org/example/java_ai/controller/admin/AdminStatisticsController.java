package org.example.java_ai.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.aspect.AiMonitorAspect;
import org.example.java_ai.service.ProductService;
import org.example.java_ai.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;
    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;
    private final AiMonitorAspect aiMonitorAspect;

    /**
     * 获取总览统计数据
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        log.info("获取统计数据");

        // 总营收（已支付、已发货、已完成订单的总金额）
        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status IN (1, 2, 3) AND deleted = 0", BigDecimal.class);

        // 今日订单数
        Integer todayOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE DATE(create_time) = CURDATE() AND deleted = 0", Integer.class);

        // 总订单数
        Long totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE deleted = 0", Long.class);

        // 待处理订单（待支付+已支付未发货）
        Long pendingOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE status IN (0, 1) AND deleted = 0", Long.class);

        // 商品总数
        Long totalProducts = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM product WHERE deleted = 0", Long.class);

        // 已上架商品数
        Long publishedProducts = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM product WHERE publish_status = 1 AND deleted = 0", Long.class);

        // 未上架商品数（草稿 + 已下架）
        Long unpublishedProducts = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM product WHERE publish_status IN (0, 2) AND deleted = 0", Long.class);

        // 总浏览量
        Long totalViews = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(view_count), 0) FROM product WHERE deleted = 0", Long.class);

        // 总销量
        Long totalSales = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(sales), 0) FROM product WHERE deleted = 0", Long.class);

        // AI分析覆盖率（有评论的商品数 / 已上架商品数）
        Long aiAnalyzedProducts = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT p.id) FROM product p INNER JOIN product_comment c ON p.id = c.product_id " +
            "WHERE p.publish_status = 1 AND p.deleted = 0 AND c.deleted = 0", Long.class);
        double aiCoverage = publishedProducts != null && publishedProducts > 0 
            ? Math.round((aiAnalyzedProducts * 100.0) / publishedProducts) 
            : 0.0;

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                "todayOrders", todayOrders != null ? todayOrders : 0,
                "totalOrders", totalOrders != null ? totalOrders : 0,
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "totalProducts", totalProducts != null ? totalProducts : 0,
                "publishedProducts", publishedProducts != null ? publishedProducts : 0,
                "unpublishedProducts", unpublishedProducts != null ? unpublishedProducts : 0,
                "totalViews", totalViews != null ? totalViews : 0,
                "totalSales", totalSales != null ? totalSales : 0,
                "aiCoverage", aiCoverage
            )
        ));
    }

    /**
     * 获取浏览量 TOP10 商品
     */
    @GetMapping("/top-views")
    public ResponseEntity<Map<String, Object>> getTopViewedProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        String sql = """
            SELECT id, name, image, view_count, click_count, sales, price
            FROM product
            WHERE deleted = 0
            ORDER BY view_count DESC
            LIMIT ?
            """;

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql, limit);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", products
        ));
    }

    /**
     * 获取点击量 TOP10 商品
     */
    @GetMapping("/top-clicks")
    public ResponseEntity<Map<String, Object>> getTopClickedProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        String sql = """
            SELECT id, name, image, view_count, click_count, sales, price
            FROM product
            WHERE deleted = 0
            ORDER BY click_count DESC
            LIMIT ?
            """;

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql, limit);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", products
        ));
    }

    /**
     * 获取销量 TOP10 商品
     */
    @GetMapping("/top-sales")
    public ResponseEntity<Map<String, Object>> getTopSoldProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        String sql = """
            SELECT id, name, image, view_count, click_count, sales, price
            FROM product
            WHERE deleted = 0
            ORDER BY sales DESC
            LIMIT ?
            """;

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql, limit);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", products
        ));
    }

    /**
     * 获取商品发布状态统计
     */
    @GetMapping("/publish-status")
    public ResponseEntity<Map<String, Object>> getPublishStatusStats() {
        String sql = """
            SELECT 
                publish_status,
                COUNT(*) as count
            FROM product
            WHERE deleted = 0
            GROUP BY publish_status
            """;

        List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql);

        // 转换为更易读的格式
        Map<Integer, Long> statusMap = new java.util.HashMap<>();
        statusMap.put(0, 0L);
        statusMap.put(1, 0L);
        statusMap.put(2, 0L);
        for (Map<String, Object> row : stats) {
            Integer status = ((Number) row.get("publish_status")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            statusMap.put(status, count);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "draft", statusMap.get(0),      // 草稿
                "published", statusMap.get(1),   // 已上架
                "unpublished", statusMap.get(2)  // 已下架
            )
        ));
    }

    /**
     * 记录商品浏览（前端调用）
     */
    @PostMapping("/product/{id}/view")
    public ResponseEntity<Map<String, Object>> recordView(@PathVariable Long id) {
        boolean success = productService.incrementViewCount(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * 记录商品点击（前端调用）
     */
    @PostMapping("/product/{id}/click")
    public ResponseEntity<Map<String, Object>> recordClick(@PathVariable Long id) {
        boolean success = productService.incrementClickCount(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * 获取AI监控统计数据
     */
    @GetMapping("/ai-monitor")
    public ResponseEntity<Map<String, Object>> getAiMonitorStats() {
        log.info("获取AI监控统计数据");
        Map<String, Object> stats = aiMonitorAspect.getStatistics();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", stats
        ));
    }
}
