package org.example.java_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据统计服务 — 封装仪表盘所有 SQL 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;

    /** 概览数据 */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalProducts", queryCount("SELECT COUNT(*) FROM product WHERE deleted = 0"));
        overview.put("publishedProducts", queryCount("SELECT COUNT(*) FROM product WHERE publish_status = 1 AND deleted = 0"));
        overview.put("totalOrders", queryCount("SELECT COUNT(*) FROM orders WHERE deleted = 0"));
        overview.put("todayOrders", queryCount("SELECT COUNT(*) FROM orders WHERE DATE(create_time) = CURDATE() AND deleted = 0"));
        overview.put("totalUsers", queryCount("SELECT COUNT(*) FROM sys_user WHERE status = 1 AND deleted = 0"));
        overview.put("pendingOrders", queryCount("SELECT COUNT(*) FROM orders WHERE status = 0 AND deleted = 0"));
        return overview;
    }

    /** 商品浏览量 Top N */
    public List<Map<String, Object>> getTopViewedProducts(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT p.id, p.name, COUNT(v.id) AS view_count FROM product_view_log v " +
            "JOIN product p ON v.product_id = p.id WHERE p.deleted = 0 GROUP BY p.id, p.name " +
            "ORDER BY view_count DESC LIMIT ?", limit);
    }

    /** 商品点击量 Top N */
    public List<Map<String, Object>> getTopClickedProducts(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT p.id, p.name, p.click_count FROM product p " +
            "WHERE p.deleted = 0 ORDER BY p.click_count DESC LIMIT ?", limit);
    }

    /** 发布状态统计 */
    public Map<String, Object> getPublishStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("published", queryCount("SELECT COUNT(*) FROM product WHERE publish_status = 1 AND deleted = 0"));
        stats.put("unpublished", queryCount("SELECT COUNT(*) FROM product WHERE publish_status = 0 AND deleted = 0"));
        return stats;
    }

    private long queryCount(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
