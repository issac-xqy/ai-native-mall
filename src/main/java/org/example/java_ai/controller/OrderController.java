package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            HttpServletRequest servletRequest,
            @RequestBody Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        String orderNo = (String) request.get("orderNo");
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) request.get("items");
        log.info("创建订单 orderNo={}, userId={}, items={}", orderNo, userId, items.size());
        return ResponseEntity.ok(orderService.createOrder(userId, orderNo, items));
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listOrders(
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", orderService.listOrders(userId, status)));
    }

    @PutMapping("/{orderNo}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(
            HttpServletRequest servletRequest,
            @PathVariable String orderNo,
            @RequestBody Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        String paymentMethod = (String) request.get("paymentMethod");
        return ResponseEntity.ok(orderService.payOrder(userId, orderNo, paymentMethod));
    }

    @PutMapping("/{orderNo}/confirm")
    public ResponseEntity<Map<String, Object>> confirmOrder(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.confirmOrder(orderNo));
    }

    @PutMapping("/{orderNo}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        String company = request != null ? (String) request.get("logisticsCompany") : null;
        String tracking = request != null ? (String) request.get("trackingNo") : null;
        return ResponseEntity.ok(orderService.shipOrder(orderNo, company, tracking));
    }

    @PutMapping("/{orderNo}/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(
            @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        String reason = request != null ? (String) request.get("reason") : "用户申请退款";
        return ResponseEntity.ok(orderService.refundOrder(orderNo, reason));
    }

    @PutMapping("/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.cancelOrder(orderNo));
    }
}
