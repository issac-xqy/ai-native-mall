package org.example.java_ai.service;

import org.example.java_ai.entity.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(Long userId, String orderNo, List<Map<String, Object>> items);

    Map<String, Object> payOrder(Long userId, String orderNo, String paymentMethod);

    Map<String, Object> confirmOrder(Long userId, String orderNo);

    Map<String, Object> shipOrder(Long userId, String orderNo, String logisticsCompany, String trackingNo);

    Map<String, Object> refundOrder(Long userId, String orderNo, String reason);

    Map<String, Object> cancelOrder(Long userId, String orderNo);

    List<Map<String, Object>> listOrders(Long userId, Integer status);
}
