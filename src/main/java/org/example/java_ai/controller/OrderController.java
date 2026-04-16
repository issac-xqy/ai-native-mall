package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.example.java_ai.service.UserWalletService;
import org.example.java_ai.entity.UserWallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final JdbcTemplate jdbcTemplate;
    private final UserWalletService walletService;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            HttpServletRequest servletRequest,
            @RequestBody Map<String, Object> request) {
        log.info("========== 创建订单请求 ==========");
        log.info("请求参数: {}", request);
        try {
            String orderNo = (String) request.get("orderNo");
            // 从拦截器中获取真实用户ID
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            
            log.info("订单号: {}, 用户ID: {}, 商品数量: {}", orderNo, userId, items.size());

            // 计算总金额
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Map<String, Object> item : items) {
                log.info("商品项: {}", item);
                BigDecimal price = new BigDecimal(item.get("price").toString());
                Integer quantity = ((Number) item.get("quantity")).intValue();
                totalAmount = totalAmount.add(price.multiply(new BigDecimal(quantity)));
            }

            log.info("总金额: {}", totalAmount);

            // 校验商品上架状态和库存
            for (Map<String, Object> item : items) {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();
                
                // 查询商品信息
                String productSql = "SELECT id, name, publish_status, stock FROM product WHERE id = ? AND deleted = 0";
                List<Map<String, Object>> productList = jdbcTemplate.queryForList(productSql, productId);
                
                if (productList.isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "商品不存在或已下架"
                    ));
                }
                
                Map<String, Object> product = productList.get(0);
                Integer publishStatus = (Integer) product.get("publish_status");
                Integer stock = (Integer) product.get("stock");
                
                // 校验上架状态
                if (publishStatus == null || publishStatus != 1) {
                    String productName = (String) product.get("name");
                    log.warn("商品未上架，无法购买 - 商品ID: {}, 名称: {}, 发布状态: {}", productId, productName, publishStatus);
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "商品【" + productName + "】未上架，暂时无法购买"
                    ));
                }
                
                // 校验库存
                if (stock == null || stock < quantity) {
                    String productName = (String) product.get("name");
                    log.warn("商品库存不足 - 商品ID: {}, 名称: {}, 请求数量: {}, 当前库存: {}", productId, productName, quantity, stock);
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "商品【" + productName + "】库存不足，仅剩" + (stock != null ? stock : 0) + "件"
                    ));
                }
            }

            // 插入订单（状态 0 代表待支付）
            String orderSql = "INSERT INTO orders (order_no, user_id, total_amount, status, create_time) VALUES (?, ?, ?, 0, NOW())";
            jdbcTemplate.update(orderSql, orderNo, userId, totalAmount);

            // 获取订单ID
            Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            log.info("订单创建成功，订单ID: {}", orderId);

            // 插入订单商品
            String itemSql = "INSERT INTO order_item (order_id, product_id, product_name, price, quantity, total_amount, create_time) VALUES (?, ?, ?, ?, ?, ?, NOW())";
            for (Map<String, Object> item : items) {
                Long productId = ((Number) item.get("productId")).longValue();
                String productName = (String) item.get("productName");
                BigDecimal price = new BigDecimal(item.get("price").toString());
                Integer quantity = ((Number) item.get("quantity")).intValue();
                BigDecimal itemTotal = price.multiply(new BigDecimal(quantity));

                jdbcTemplate.update(itemSql, orderId, productId, productName, price, quantity, itemTotal);

                // 更新商品销量和库存
                jdbcTemplate.update("UPDATE product SET sales = sales + ?, stock = stock - ? WHERE id = ?", 
                    quantity, quantity, productId);
            }

            log.info("订单创建完成 - 订单号: {}, 金额: {}", orderNo, totalAmount);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "下单成功",
                "orderId", orderId,
                "orderNo", orderNo
            ));

        } catch (Exception e) {
            log.error("创建订单失败 - 详细错误:", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "下单失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listOrders(
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Integer status) {
        try {
            // 从拦截器中获取真实用户ID
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            
            // 构建SQL，根据status参数动态过滤
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
                    WHERE o.user_id = ? AND o.status = ? AND o.deleted = 0
                    ORDER BY o.create_time DESC
                    """;
                params = new Object[]{userId, status};
                log.info("查询订单列表 - 用户ID: {}, 状态过滤: {}", userId, status);
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
                    WHERE o.user_id = ? AND o.deleted = 0
                    ORDER BY o.create_time DESC
                    """;
                params = new Object[]{userId};
                log.info("查询订单列表 - 用户ID: {}, 全部状态", userId);
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

            // 组装订单数据
            Map<Long, Map<String, Object>> orderMap = new java.util.LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Long orderId = ((Number) row.get("id")).longValue();
                
                if (!orderMap.containsKey(orderId)) {
                    Map<String, Object> order = new java.util.LinkedHashMap<>();
                    order.put("id", orderId);
                    order.put("orderNo", row.get("order_no"));
                    order.put("totalAmount", row.get("total_amount"));
                    order.put("status", row.get("status"));
                    order.put("createTime", row.get("create_time"));
                    order.put("items", new java.util.ArrayList<Map<String, Object>>());
                    orderMap.put(orderId, order);
                }

                if (row.get("item_id") != null) {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
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
                "data", new java.util.ArrayList<>(orderMap.values())
            ));

        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "查询失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 支付订单
     */
    @PutMapping("/{orderNo}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(
            HttpServletRequest servletRequest,
            @PathVariable String orderNo,
            @RequestBody Map<String, Object> request) {
        try {
            // 从拦截器中获取用户ID
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            
            String paymentMethod = (String) request.get("paymentMethod");
            
            // 1. 获取订单信息
            String orderSql = "SELECT id, user_id, total_amount, status FROM orders WHERE order_no = ? AND deleted = 0";
            List<Map<String, Object>> orderList = jdbcTemplate.queryForList(orderSql, orderNo);
            
            if (orderList.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单不存在"
                ));
            }
            
            Map<String, Object> order = orderList.get(0);
            Integer status = (Integer) order.get("status");
            
            // 只有待支付的订单可以支付
            if (status != 0) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单已支付或状态异常"
                ));
            }
            
            BigDecimal totalAmount = (BigDecimal) order.get("total_amount");
            
            // 2. 检查钱包余额是否充足
            UserWallet wallet = walletService.getOrCreateWallet(userId);
            if (wallet.getBalance().compareTo(totalAmount) < 0) {
                BigDecimal needAmount = totalAmount.subtract(wallet.getBalance());
                log.warn("余额不足 - 用户ID: {}, 钱包余额: {}, 订单金额: {}, 需充值: {}", 
                        userId, wallet.getBalance(), totalAmount, needAmount);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "钱包余额不足，还差¥" + needAmount + "，请充值后再支付",
                    "needRecharge", needAmount
                ));
            }
            
            // 3. 扣款
            boolean deductSuccess = walletService.deductBalance(userId, totalAmount, "订单支付-" + orderNo);
            if (!deductSuccess) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "支付失败，请重试"
                ));
            }
            
            // 4. 更新订单状态为已支付
            String updateSql = "UPDATE orders SET status = 1 WHERE order_no = ? AND status = 0";
            int rows = jdbcTemplate.update(updateSql, orderNo);
            
            if (rows > 0) {
                log.info("订单支付成功 - 订单号: {}, 支付方式: {}, 扣款金额: {}", orderNo, paymentMethod, totalAmount);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "支付成功",
                    "newBalance", walletService.getBalance(userId)
                ));
            } else {
                // 如果更新订单失败，需要回滚扣款
                walletService.refund(userId, totalAmount, "支付失败退款-" + orderNo);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单不存在或已支付"
                ));
            }
        } catch (Exception e) {
            log.error("订单支付失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "支付失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 确认收货
     */
    @PutMapping("/{orderNo}/confirm")
    public ResponseEntity<Map<String, Object>> confirmOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            // 只有已发货的订单可以确认收货
            String sql = "UPDATE orders SET status = 3 WHERE order_no = ? AND status = 2";
            int rows = jdbcTemplate.update(sql, orderNo);
            
            if (rows > 0) {
                log.info("确认收货成功 - 订单号: {}", orderNo);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "确认收货成功"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单状态不允许确认收货"
                ));
            }
        } catch (Exception e) {
            log.error("确认收货失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "确认收货失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 订单发货（管理后台调用）
     */
    @PutMapping("/{orderNo}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            // 获取订单信息
            String orderSql = "SELECT id, status FROM orders WHERE order_no = ? AND deleted = 0";
            List<Map<String, Object>> orderList = jdbcTemplate.queryForList(orderSql, orderNo);
            
            if (orderList.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单不存在"
                ));
            }
            
            Map<String, Object> order = orderList.get(0);
            Integer status = (Integer) order.get("status");
            
            // 只有已支付的订单可以发货
            if (status != 1) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "只有已支付订单才能发货"
                ));
            }
            
            // 获取物流信息（可选）
            String logisticsCompany = request != null ? (String) request.get("logisticsCompany") : null;
            String trackingNo = request != null ? (String) request.get("trackingNo") : null;
            
            // 更新订单状态为已发货
            String updateSql = "UPDATE orders SET status = 2 WHERE order_no = ?";
            int rows = jdbcTemplate.update(updateSql, orderNo);
            
            if (rows > 0) {
                log.info("订单发货成功 - 订单号: {}, 物流公司: {}, 物流单号: {}", 
                    orderNo, logisticsCompany, trackingNo);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "发货成功",
                    "logisticsCompany", logisticsCompany,
                    "trackingNo", trackingNo
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "发货失败"
                ));
            }
        } catch (Exception e) {
            log.error("订单发货失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "发货失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 申请退款
     */
    @PutMapping("/{orderNo}/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            // 获取订单信息
            String orderSql = "SELECT id, user_id, status FROM orders WHERE order_no = ? AND deleted = 0";
            List<Map<String, Object>> orderList = jdbcTemplate.queryForList(orderSql, orderNo);
            
            if (orderList.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单不存在"
                ));
            }
            
            Map<String, Object> order = orderList.get(0);
            Integer status = (Integer) order.get("status");
            
            // 只有已支付和已发货状态的订单可以申请退款
            if (status != 1 && status != 2) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单状态不允许申请退款"
                ));
            }
            
            // 获取退款原因
            String refundReason = request != null ? (String) request.get("reason") : "用户申请退款";
            
            // 恢复商品库存和销量
            String itemSql = "SELECT product_id, quantity FROM order_item WHERE order_id = ?";
            List<Map<String, Object>> items = jdbcTemplate.queryForList(itemSql, order.get("id"));
            
            for (Map<String, Object> item : items) {
                Long productId = ((Number) item.get("product_id")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();
                
                // 恢复库存
                jdbcTemplate.update("UPDATE product SET stock = stock + ?, sales = sales - ? WHERE id = ?", 
                    quantity, quantity, productId);
            }
            
            // 更新订单状态为已取消（退款完成）
            String updateSql = "UPDATE orders SET status = 4 WHERE order_no = ?";
            int rows = jdbcTemplate.update(updateSql, orderNo);
            
            if (rows > 0) {
                log.info("订单退款成功 - 订单号: {}, 退款原因: {}", orderNo, refundReason);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款成功",
                    "refundReason", refundReason
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "退款失败"
                ));
            }
        } catch (Exception e) {
            log.error("订单退款失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "退款失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderNo) {
        try {
            // 获取订单信息
            String orderSql = "SELECT id, user_id, status FROM orders WHERE order_no = ? AND deleted = 0";
            List<Map<String, Object>> orderList = jdbcTemplate.queryForList(orderSql, orderNo);
            
            if (orderList.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "订单不存在"
                ));
            }
            
            Map<String, Object> order = orderList.get(0);
            Integer status = (Integer) order.get("status");
            
            // 只有待支付状态的订单可以直接取消
            // 已支付状态的订单应走退款流程，不允许直接取消
            if (status != 0) {
                String message = status == 1 ? "已支付订单请申请退款，不允许直接取消" : "订单状态不允许取消";
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", message
                ));
            }
            
            // 恢复商品库存和销量
            String itemSql = "SELECT product_id, quantity FROM order_item WHERE order_id = ?";
            List<Map<String, Object>> items = jdbcTemplate.queryForList(itemSql, order.get("id"));
            
            for (Map<String, Object> item : items) {
                Long productId = ((Number) item.get("product_id")).longValue();
                Integer quantity = ((Number) item.get("quantity")).intValue();
                
                // 恢复库存
                jdbcTemplate.update("UPDATE product SET stock = stock + ?, sales = sales - ? WHERE id = ?", 
                    quantity, quantity, productId);
            }
            
            // 更新订单状态为已取消
            String updateSql = "UPDATE orders SET status = 4 WHERE order_no = ?";
            int rows = jdbcTemplate.update(updateSql, orderNo);
            
            if (rows > 0) {
                log.info("订单取消成功 - 订单号: {}", orderNo);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单已取消"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "取消失败"
                ));
            }
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "取消失败: " + e.getMessage()
            ));
        }
    }
}
