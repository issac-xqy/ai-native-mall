package org.example.java_ai.controller;

import org.example.java_ai.service.OrderService;
import org.example.java_ai.util.TokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OrderController.class,
        excludeAutoConfiguration = {
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@DisplayName("OrderController MockMvc 测试")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;
    private final String token = TokenUtil.generateToken(1L);

    @Test
    @DisplayName("创建订单-正常下单-返回订单信息")
    void createOrder_Valid_ReturnsOrder() throws Exception {
        when(orderService.createOrder(eq(1L), eq("ORD001"), anyList()))
                .thenReturn(Map.of("success", true, "orderId", 1, "orderNo", "ORD001"));

        mockMvc.perform(post("/order")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderNo":"ORD001","items":[{"productId":100,"productName":"iPhone","price":5999,"quantity":1}]}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("创建订单-无Token-返回未登录")
    void createOrder_NoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"ORD001\",\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("支付-正常支付-返回成功")
    void payOrder_Success() throws Exception {
        when(orderService.payOrder(eq(1L), eq("ORD001"), eq("wallet")))
                .thenReturn(Map.of("success", true, "message", "支付成功"));

        mockMvc.perform(put("/order/ORD001/pay")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"wallet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("支付-订单不存在-返回失败")
    void payOrder_NotFound() throws Exception {
        when(orderService.payOrder(eq(1L), eq("NOT_EXIST"), any()))
                .thenReturn(Map.of("success", false, "message", "订单不存在"));

        mockMvc.perform(put("/order/NOT_EXIST/pay")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"wallet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.success").value(false));
    }

    @Test
    @DisplayName("确认收货-成功")
    void confirmOrder_Success() throws Exception {
        when(orderService.confirmOrder(eq(1L), eq("ORD001")))
                .thenReturn(Map.of("success", true, "message", "确认收货成功"));

        mockMvc.perform(put("/order/ORD001/confirm")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("取消订单-待支付-成功")
    void cancelOrder_Pending_Success() throws Exception {
        when(orderService.cancelOrder(eq(1L), eq("ORD002")))
                .thenReturn(Map.of("success", true, "message", "订单已取消"));

        mockMvc.perform(put("/order/ORD002/cancel")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("取消订单-已支付-不允许")
    void cancelOrder_AlreadyPaid() throws Exception {
        when(orderService.cancelOrder(eq(1L), eq("ORD003")))
                .thenReturn(Map.of("success", false, "message", "已支付订单请申请退款"));

        mockMvc.perform(put("/order/ORD003/cancel")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.success").value(false));
    }

    @Test
    @DisplayName("退款-已支付-成功")
    void refundOrder_Paid_Success() throws Exception {
        when(orderService.refundOrder(eq(1L), eq("ORD004"), any()))
                .thenReturn(Map.of("success", true, "message", "退款成功"));

        mockMvc.perform(put("/order/ORD004/refund")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"质量问题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("发货-已支付-成功")
    void shipOrder_Paid_Success() throws Exception {
        when(orderService.shipOrder(eq(1L), eq("ORD005"), eq("顺丰"), eq("SF123")))
                .thenReturn(Map.of("success", true, "logisticsCompany", "顺丰"));

        mockMvc.perform(put("/order/ORD005/ship")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"顺丰\",\"trackingNo\":\"SF123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询订单列表")
    void listOrders() throws Exception {
        when(orderService.listOrders(1L, null))
                .thenReturn(List.of(Map.of("id", 1, "orderNo", "ORD001")));

        mockMvc.perform(get("/order/list")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data").isArray());
    }
}
