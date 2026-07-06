package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Map<String, Object>> createOrder(
            HttpServletRequest servletRequest, @RequestBody Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        String orderNo = (String) request.get("orderNo");
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) request.get("items");
        log.info("创建订单 orderNo={}, userId={}, items={}", orderNo, userId, items.size());
        return Result.success(orderService.createOrder(userId, orderNo, items));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listOrders(
            HttpServletRequest servletRequest, @RequestParam(required = false) Integer status) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        return Result.success(Map.of("data", orderService.listOrders(userId, status)));
    }

    @PutMapping("/{orderNo}/pay")
    public Result<Map<String, Object>> payOrder(
            HttpServletRequest servletRequest, @PathVariable String orderNo,
            @RequestBody Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        String paymentMethod = (String) request.get("paymentMethod");
        return Result.success(orderService.payOrder(userId, orderNo, paymentMethod));
    }

    @PutMapping("/{orderNo}/confirm")
    public Result<Map<String, Object>> confirmOrder(
            HttpServletRequest servletRequest, @PathVariable String orderNo) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        return Result.success(orderService.confirmOrder(userId, orderNo));
    }

    @PutMapping("/{orderNo}/ship")
    public Result<Map<String, Object>> shipOrder(
            HttpServletRequest servletRequest, @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        String company = request != null ? (String) request.get("logisticsCompany") : null;
        String tracking = request != null ? (String) request.get("trackingNo") : null;
        return Result.success(orderService.shipOrder(userId, orderNo, company, tracking));
    }

    @PutMapping("/{orderNo}/refund")
    public Result<Map<String, Object>> refundOrder(
            HttpServletRequest servletRequest, @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Object> request) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        String reason = request != null ? (String) request.get("reason") : "用户申请退款";
        return Result.success(orderService.refundOrder(userId, orderNo, reason));
    }

    @PutMapping("/{orderNo}/cancel")
    public Result<Map<String, Object>> cancelOrder(
            HttpServletRequest servletRequest, @PathVariable String orderNo) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        return Result.success(orderService.cancelOrder(userId, orderNo));
    }
}
