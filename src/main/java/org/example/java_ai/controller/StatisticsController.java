package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.service.StatisticsService;
import org.example.java_ai.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final ProductService productService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = statisticsService.getOverview();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取统计数据失败"));
        }
    }

    @GetMapping("/product-ranking")
    public ResponseEntity<Map<String, Object>> getProductRanking() {
        try {
            return ResponseEntity.ok(Map.of("success", true,
                "data", productService.getTopSalesProducts(10)));
        } catch (Exception e) {
            log.error("获取商品排行失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取商品排行失败"));
        }
    }
}
