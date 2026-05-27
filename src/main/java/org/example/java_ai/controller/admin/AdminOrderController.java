package org.example.java_ai.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台订单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询订单列表（管理后台）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listOrders(
            @RequestParam(required = false) Integer status) {
        try {
            String sql;
            Object[] params;

            if (status != null) {
                sql = """
                    SELECT
                        o.id,
                        o.order_no,
                        o.total_amount,
                        o.status,
                        o.create_time,
                        oi.id as item_id,
                        oi.product_id,
                        oi.product_name,
                        oi.price,
                        oi.quantity,
                        oi.total_amount as item_total
                    FROM orders o
                    LEFT JOIN order_item oi ON o.id = oi.order_id
                    WHERE o.status = ? AND o.deleted = 0
                    ORDER BY o.create_time DESC
                    """;
                params = new Object[]{status};
            } else {
                sql = """
                    SELECT
                        o.id,
                        o.order_no,
                        o.total_amount,
                        o.status,
                        o.create_time,
                        oi.id as item_id,
                        oi.product_id,
                        oi.product_name,
                        oi.price,
                        oi.quantity,
                        oi.total_amount as item_total
                    FROM orders o
                    LEFT JOIN order_item oi ON o.id = oi.order_id
                    WHERE o.deleted = 0
                    ORDER BY o.create_time DESC
                    """;
                params = new Object[]{};
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

            // 组装订单数据
            Map<Long, Map<String, Object>> orderMap = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Long orderId = ((Number) row.get("id")).longValue();

                if (!orderMap.containsKey(orderId)) {
                    Map<String, Object> order = new LinkedHashMap<>();
                    order.put("id", orderId);
                    order.put("orderNo", row.get("order_no"));
                    order.put("totalAmount", row.get("total_amount"));
                    order.put("status", row.get("status"));
                    order.put("createTime", row.get("create_time"));
                    order.put("items", new ArrayList<Map<String, Object>>());
                    orderMap.put(orderId, order);
                }

                if (row.get("item_id") != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", row.get("item_id"));
                    item.put("productId", row.get("product_id"));
                    item.put("productName", row.get("product_name"));
                    item.put("price", row.get("price"));
                    item.put("quantity", row.get("quantity"));
                    item.put("totalAmount", row.get("item_total"));

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) orderMap.get(orderId).get("items");
                    items.add(item);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", new ArrayList<>(orderMap.values())
            ));

        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "查询失败: " + e.getMessage()
            ));
        }
    }
}
