package org.example.java_ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.OrderStatus;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.entity.Order;
import org.example.java_ai.entity.OrderItem;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.mapper.OrderItemMapper;
import org.example.java_ai.mapper.OrderMapper;
import org.example.java_ai.service.OrderService;
import org.example.java_ai.service.UserWalletService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserWalletService walletService;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, String orderNo, List<Map<String, Object>> items) {
        if (orderNo == null || orderNo.isEmpty()) {
            orderNo = "ORD" + System.currentTimeMillis() + String.format("%04d", userId);
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            BigDecimal price = toBigDecimal(item.get("price"));
            int quantity = ((Number) item.get("quantity")).intValue();
            totalAmount = totalAmount.add(price.multiply(new BigDecimal(quantity)));
        }

        // 插入订单（CHECK 约束会拦截负数金额）
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED.getCode());
        try {
            orderMapper.insert(order);
        } catch (DataAccessException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单金额无效，请检查商品价格");
        }

        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            String productName = (String) item.get("productName");
            BigDecimal price = toBigDecimal(item.get("price"));
            int quantity = ((Number) item.get("quantity")).intValue();

            // 原子扣减库存：WHERE stock >= quantity
            int affected = orderMapper.deductStock(productId, quantity);
            if (affected == 0) {
                throw new BusinessException(ResultCode.INSUFFICIENT_STOCK,
                    "商品【" + productName + "】库存不足");
            }

            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(productId);
            oi.setProductName(productName);
            oi.setPrice(price);
            oi.setQuantity(quantity);
            oi.setTotalAmount(price.multiply(new BigDecimal(quantity)));
            orderItemMapper.insert(oi);
        }

        log.info("订单创建成功 orderId={}, orderNo={}, amount={}", order.getId(), orderNo, totalAmount);
        return Map.of("success", true, "message", "下单成功", "orderId", order.getId(), "orderNo", orderNo);
    }

    @Override
    @Transactional
    public Map<String, Object> payOrder(Long userId, String orderNo, String paymentMethod) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return fail("订单不存在");
        if (order.getStatus() != OrderStatus.CREATED.getCode()) return fail("订单已支付或状态异常");

        UserWallet wallet = walletService.getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(order.getTotalAmount()) < 0) {
            BigDecimal need = order.getTotalAmount().subtract(wallet.getBalance());
            return Map.of("success", false, "message", "钱包余额不足，还差¥" + need + "，请充值后再支付",
                    "needRecharge", need);
        }

        if (!walletService.deductBalance(userId, order.getTotalAmount(), "订单支付-" + orderNo)) {
            return fail("支付失败，请重试");
        }

        // 原子更新订单状态
        int updated = orderMapper.updateStatus(orderNo, OrderStatus.CREATED.getCode(), OrderStatus.PAID.getCode());
        if (updated == 0) {
            // 扣款成功但状态更新失败，退还余额
            walletService.refund(userId, order.getTotalAmount(), "支付失败退款-" + orderNo);
            return fail("订单状态异常，已退还余额");
        }

        log.info("支付成功 orderNo={}, amount={}", orderNo, order.getTotalAmount());
        return Map.of("success", true, "message", "支付成功", "newBalance", walletService.getBalance(userId));
    }

    @Override
    public Map<String, Object> confirmOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return fail("订单不存在");
        if (!order.getUserId().equals(userId)) return fail("无权操作该订单");
        int rows = orderMapper.updateStatus(orderNo, OrderStatus.SHIPPED.getCode(), OrderStatus.COMPLETED.getCode());
        return rows > 0 ? Map.of("success", true, "message", "确认收货成功")
                : fail("订单状态不允许确认收货");
    }

    @Override
    public Map<String, Object> shipOrder(Long userId, String orderNo, String logisticsCompany, String trackingNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return fail("订单不存在");
        if (order.getStatus() != OrderStatus.PAID.getCode()) return fail("只有已支付订单才能发货");
        order.setStatus(OrderStatus.SHIPPED.getCode());
        orderMapper.updateById(order);
        log.info("发货成功 orderNo={}, company={}, tracking={}, operator={}", orderNo, logisticsCompany, trackingNo, userId);
        return Map.of("success", true, "message", "发货成功",
                "logisticsCompany", logisticsCompany, "trackingNo", trackingNo);
    }

    @Override
    @Transactional
    public Map<String, Object> refundOrder(Long userId, String orderNo, String reason) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return fail("订单不存在");
        if (!order.getUserId().equals(userId)) return fail("无权操作该订单");
        int currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PAID.getCode() && currentStatus != OrderStatus.SHIPPED.getCode()) {
            return fail("订单状态不允许申请退款");
        }
        walletService.refund(order.getUserId(), order.getTotalAmount(), "订单退款-" + orderNo);
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        if (items != null) {
            for (OrderItem oi : items) {
                orderMapper.restoreStock(oi.getProductId(), oi.getQuantity());
            }
        }
        int updated = orderMapper.updateStatus(orderNo, currentStatus, OrderStatus.CANCELLED.getCode());
        if (updated == 0) return fail("退款处理失败，订单状态已变更");
        log.info("退款成功 orderNo={}, reason={}", orderNo, reason);
        return Map.of("success", true, "message", "退款成功", "refundReason", reason);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return fail("订单不存在");
        if (!order.getUserId().equals(userId)) return fail("无权操作该订单");
        if (order.getStatus() != OrderStatus.CREATED.getCode()) {
            String msg = order.getStatus() == OrderStatus.PAID.getCode() ? "已支付订单请申请退款，不允许直接取消" : "订单状态不允许取消";
            return fail(msg);
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        if (items != null) {
            for (OrderItem oi : items) {
                orderMapper.restoreStock(oi.getProductId(), oi.getQuantity());
            }
        }
        orderMapper.updateStatus(orderNo, OrderStatus.CREATED.getCode(), OrderStatus.CANCELLED.getCode());
        return Map.of("success", true, "message", "订单已取消");
    }

    @Override
    public List<Map<String, Object>> listOrders(Long userId, Integer status) {
        List<Order> orders;
        if (status != null) {
            orders = orderMapper.selectByUserIdAndStatus(userId, status);
        } else {
            orders = orderMapper.selectByUserId(userId);
        }

        // 批量查询订单商品（修复 N+1 查询）
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> allItems = orderItemMapper.selectByOrderIds(orderIds);
            for (OrderItem item : allItems) {
                orderItemsMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new LinkedHashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("createTime", order.getCreateTime());

            List<OrderItem> items = orderItemsMap.getOrDefault(order.getId(), List.of());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (OrderItem oi : items) {
                itemList.add(Map.of(
                        "id", oi.getId(),
                        "productId", oi.getProductId(),
                        "productName", oi.getProductName() != null ? oi.getProductName() : "",
                        "price", oi.getPrice(),
                        "quantity", oi.getQuantity(),
                        "totalAmount", oi.getTotalAmount()
                ));
            }
            orderMap.put("items", itemList);
            result.add(orderMap);
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private Map<String, Object> fail(String message) {
        return Map.of("success", false, "message", message);
    }
}
