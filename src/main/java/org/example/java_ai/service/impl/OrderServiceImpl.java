package org.example.java_ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.Order;
import org.example.java_ai.entity.OrderItem;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.mapper.OrderItemMapper;
import org.example.java_ai.mapper.OrderMapper;
import org.example.java_ai.service.OrderService;
import org.example.java_ai.service.UserWalletService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final JdbcTemplate jdbcTemplate;
    private final UserWalletService walletService;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, String orderNo, List<Map<String, Object>> items) {
        // 计算总金额 & 校验商品
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            BigDecimal price = new BigDecimal(item.get("price").toString());
            int quantity = ((Number) item.get("quantity")).intValue();
            totalAmount = totalAmount.add(price.multiply(new BigDecimal(quantity)));

            var rows = jdbcTemplate.queryForList(
                    "SELECT name, publish_status, stock FROM product WHERE id = ? AND deleted = 0", productId);
            if (rows.isEmpty()) return fail("商品不存在或已下架");

            var product = rows.get(0);
            int publishStatus = (int) product.get("publish_status");
            int stock = (int) product.get("stock");

            if (publishStatus != 1) return fail("商品【" + product.get("name") + "】未上架，暂时无法购买");
            if (stock < quantity) return fail("商品【" + product.get("name") + "】库存不足，仅剩" + stock + "件");
        }

        // 插入订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        orderMapper.insert(order);

        // 插入订单商品 & 更新库存
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            String productName = (String) item.get("productName");
            BigDecimal price = new BigDecimal(item.get("price").toString());
            int quantity = ((Number) item.get("quantity")).intValue();

            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(productId);
            oi.setProductName(productName);
            oi.setPrice(price);
            oi.setQuantity(quantity);
            oi.setTotalAmount(price.multiply(new BigDecimal(quantity)));
            orderItemMapper.insert(oi);

            jdbcTemplate.update("UPDATE product SET sales = sales + ?, stock = stock - ? WHERE id = ?",
                    quantity, quantity, productId);
        }

        log.info("订单创建成功 orderId={}, orderNo={}, amount={}", order.getId(), orderNo, totalAmount);
        return Map.of("success", true, "message", "下单成功", "orderId", order.getId(), "orderNo", orderNo);
    }

    @Override
    @Transactional
    public Map<String, Object> payOrder(Long userId, String orderNo, String paymentMethod) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, user_id, total_amount, status FROM orders WHERE order_no = ? AND deleted = 0", orderNo);
        if (rows.isEmpty()) return fail("订单不存在");

        var row = rows.get(0);
        int status = (int) row.get("status");
        if (status != 0) return fail("订单已支付或状态异常");

        BigDecimal totalAmount = (BigDecimal) row.get("total_amount");

        UserWallet wallet = walletService.getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            BigDecimal need = totalAmount.subtract(wallet.getBalance());
            return Map.of("success", false, "message", "钱包余额不足，还差¥" + need + "，请充值后再支付",
                    "needRecharge", need);
        }

        if (!walletService.deductBalance(userId, totalAmount, "订单支付-" + orderNo)) {
            return fail("支付失败，请重试");
        }

        int updated = jdbcTemplate.update("UPDATE orders SET status = 1 WHERE order_no = ? AND status = 0", orderNo);
        if (updated == 0) {
            walletService.refund(userId, totalAmount, "支付失败退款-" + orderNo);
            return fail("订单不存在或已支付");
        }

        log.info("支付成功 orderNo={}, amount={}", orderNo, totalAmount);
        return Map.of("success", true, "message", "支付成功", "newBalance", walletService.getBalance(userId));
    }

    @Override
    public Map<String, Object> confirmOrder(String orderNo) {
        int rows = jdbcTemplate.update("UPDATE orders SET status = 3 WHERE order_no = ? AND status = 2", orderNo);
        return rows > 0 ? Map.of("success", true, "message", "确认收货成功")
                : fail("订单状态不允许确认收货");
    }

    @Override
    public Map<String, Object> shipOrder(String orderNo, String logisticsCompany, String trackingNo) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, status FROM orders WHERE order_no = ? AND deleted = 0", orderNo);
        if (rows.isEmpty()) return fail("订单不存在");

        int status = (int) rows.get(0).get("status");
        if (status != 1) return fail("只有已支付订单才能发货");

        jdbcTemplate.update("UPDATE orders SET status = 2 WHERE order_no = ?", orderNo);
        log.info("发货成功 orderNo={}, company={}, tracking={}", orderNo, logisticsCompany, trackingNo);
        return Map.of("success", true, "message", "发货成功",
                "logisticsCompany", logisticsCompany, "trackingNo", trackingNo);
    }

    @Override
    @Transactional
    public Map<String, Object> refundOrder(String orderNo, String reason) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, user_id, status FROM orders WHERE order_no = ? AND deleted = 0", orderNo);
        if (rows.isEmpty()) return fail("订单不存在");

        int status = (int) rows.get(0).get("status");
        if (status != 1 && status != 2) return fail("订单状态不允许申请退款");

        Long orderId = ((Number) rows.get(0).get("id")).longValue();

        // 恢复库存
        var items = jdbcTemplate.queryForList("SELECT product_id, quantity FROM order_item WHERE order_id = ?", orderId);
        for (var item : items) {
            Long productId = ((Number) item.get("product_id")).longValue();
            int quantity = (int) item.get("quantity");
            jdbcTemplate.update("UPDATE product SET stock = stock + ?, sales = sales - ? WHERE id = ?",
                    quantity, quantity, productId);
        }

        jdbcTemplate.update("UPDATE orders SET status = 4 WHERE order_no = ?", orderNo);
        log.info("退款成功 orderNo={}, reason={}", orderNo, reason);
        return Map.of("success", true, "message", "退款成功", "refundReason", reason);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOrder(String orderNo) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, user_id, status FROM orders WHERE order_no = ? AND deleted = 0", orderNo);
        if (rows.isEmpty()) return fail("订单不存在");

        var row = rows.get(0);
        int status = (int) row.get("status");

        if (status != 0) {
            String msg = status == 1 ? "已支付订单请申请退款，不允许直接取消" : "订单状态不允许取消";
            return fail(msg);
        }

        Long orderId = ((Number) row.get("id")).longValue();

        var items = jdbcTemplate.queryForList("SELECT product_id, quantity FROM order_item WHERE order_id = ?", orderId);
        for (var item : items) {
            Long productId = ((Number) item.get("product_id")).longValue();
            int quantity = (int) item.get("quantity");
            jdbcTemplate.update("UPDATE product SET stock = stock + ?, sales = sales - ? WHERE id = ?",
                    quantity, quantity, productId);
        }

        jdbcTemplate.update("UPDATE orders SET status = 4 WHERE order_no = ?", orderNo);
        return Map.of("success", true, "message", "订单已取消");
    }

    @Override
    public List<Map<String, Object>> listOrders(Long userId, Integer status) {
        String sql;
        Object[] params;
        if (status != null) {
            sql = """
                SELECT o.id, o.order_no, o.total_amount, o.status, o.create_time,
                       oi.id as item_id, oi.product_id, oi.product_name, oi.price, oi.quantity, oi.total_amount as item_total
                FROM orders o LEFT JOIN order_item oi ON o.id = oi.order_id
                WHERE o.user_id = ? AND o.status = ? AND o.deleted = 0 ORDER BY o.create_time DESC""";
            params = new Object[]{userId, status};
        } else {
            sql = """
                SELECT o.id, o.order_no, o.total_amount, o.status, o.create_time,
                       oi.id as item_id, oi.product_id, oi.product_name, oi.price, oi.quantity, oi.total_amount as item_total
                FROM orders o LEFT JOIN order_item oi ON o.id = oi.order_id
                WHERE o.user_id = ? AND o.deleted = 0 ORDER BY o.create_time DESC""";
            params = new Object[]{userId};
        }

        var rows = jdbcTemplate.queryForList(sql, params);

        Map<Long, Map<String, Object>> orderMap = new LinkedHashMap<>();
        for (var row : rows) {
            Long orderId = ((Number) row.get("id")).longValue();
            orderMap.computeIfAbsent(orderId, k -> {
                Map<String, Object> order = new LinkedHashMap<>();
                order.put("id", orderId);
                order.put("orderNo", row.get("order_no"));
                order.put("totalAmount", row.get("total_amount"));
                order.put("status", row.get("status"));
                order.put("createTime", row.get("create_time"));
                order.put("items", new ArrayList<>());
                return order;
            });
            if (row.get("item_id") != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", row.get("item_id"));
                item.put("productId", row.get("product_id"));
                item.put("productName", row.get("product_name"));
                item.put("price", row.get("price"));
                item.put("quantity", row.get("quantity"));
                item.put("totalAmount", row.get("item_total"));
                @SuppressWarnings("unchecked")
                var items = (List<Map<String, Object>>) orderMap.get(orderId).get("items");
                items.add(item);
            }
        }
        return new ArrayList<>(orderMap.values());
    }

    private Map<String, Object> fail(String message) {
        return Map.of("success", false, "message", message);
    }
}
