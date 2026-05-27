package org.example.java_ai.controller;

import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.service.UserWalletService;
import org.example.java_ai.util.TokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController MockMvc 测试")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private UserWalletService walletService;

    private final String token = TokenUtil.generateToken(1L);

    // ==================== POST /api/order ====================

    @Test
    @DisplayName("创建订单-正常下单-返回订单信息")
    void createOrder_ValidRequest_ReturnsOrder() throws Exception {
        // 商品库存校验：商品存在且有库存
        List<Map<String, Object>> productRow = List.of(productRow(100L, "iPhone 15", 1, 50));
        doReturn(productRow).when(jdbcTemplate).queryForList(anyString(), eq(100L));

        // 插入订单
        doReturn(1).when(jdbcTemplate).update(anyString(), eq("ORD001"), eq(1L), any(BigDecimal.class));
        // 获取自增ID
        doReturn(1L).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class));
        // 插入订单商品项
        doReturn(1).when(jdbcTemplate).update(
                anyString(), eq(1L), eq(100L), eq("iPhone 15"), any(BigDecimal.class), eq(1), any(BigDecimal.class));
        // 更新库存和销量
        doReturn(1).when(jdbcTemplate).update(anyString(), eq(1), eq(1), eq(100L));

        mockMvc.perform(post("/api/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderNo": "ORD001",
                                "items": [
                                    {"productId": 100, "productName": "iPhone 15", "price": 5999.00, "quantity": 1}
                                ]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderNo").value("ORD001"));
    }

    @Test
    @DisplayName("创建订单-商品库存不足-返回错误")
    void createOrder_InsufficientStock_ReturnsError() throws Exception {
        List<Map<String, Object>> productRow = List.of(productRow(100L, "iPhone 15", 1, 0)); // 库存为0
        doReturn(productRow).when(jdbcTemplate).queryForList(anyString(), eq(100L));

        mockMvc.perform(post("/api/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderNo": "ORD001",
                                "items": [
                                    {"productId": 100, "productName": "iPhone 15", "price": 5999.00, "quantity": 2}
                                ]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("库存不足")));
    }

    @Test
    @DisplayName("创建订单-无Token-返回401")
    void createOrder_NoToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"ORD001\",\"items\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/order/{orderNo}/pay ====================

    @Test
    @DisplayName("支付-余额充足-支付成功")
    void payOrder_SufficientBalance_Success() throws Exception {
        // 查询订单
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "5999.00", 0));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD001"));

        // 钱包余额充足
        UserWallet wallet = walletWithBalance("10000.00");
        doReturn(wallet).when(walletService).getOrCreateWallet(1L);

        // 扣款成功
        doReturn(true).when(walletService).deductBalance(eq(1L), any(BigDecimal.class), anyString());

        // 更新订单状态
        doReturn(1).when(jdbcTemplate).update(anyString(), eq("ORD001"));

        // 查询最新余额
        doReturn(new BigDecimal("4001.00")).when(walletService).getBalance(1L);

        mockMvc.perform(put("/api/order/ORD001/pay")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"paymentMethod":"wallet"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newBalance").value(4001.00));
    }

    @Test
    @DisplayName("支付-余额不足-返回差额提示")
    void payOrder_InsufficientBalance_ReturnsNeedRecharge() throws Exception {
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "5999.00", 0));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD001"));

        UserWallet wallet = walletWithBalance("1000.00");
        doReturn(wallet).when(walletService).getOrCreateWallet(1L);

        mockMvc.perform(put("/api/order/ORD001/pay")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"paymentMethod":"wallet"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.needRecharge").exists());
    }

    @Test
    @DisplayName("支付-订单不存在-返回错误")
    void payOrder_NotFound_ReturnsError() throws Exception {
        doReturn(Collections.emptyList()).when(jdbcTemplate).queryForList(anyString(), eq("NOT_EXIST"));

        mockMvc.perform(put("/api/order/NOT_EXIST/pay")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"paymentMethod":"wallet"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    // ==================== PUT /api/order/{orderNo}/confirm ====================
    // /api/order/confirm is NOT whitelisted, so it needs a token to pass the interceptor.
    // But the controller doesn't check userId - it's a public endpoint in practice.

    @Test
    @DisplayName("确认收货-订单已发货-成功")
    void confirmOrder_Shipped_Success() throws Exception {
        doReturn(1).when(jdbcTemplate).update(anyString(), eq("ORD001"));

        mockMvc.perform(put("/api/order/ORD001/confirm")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("确认收货成功"));
    }

    @Test
    @DisplayName("确认收货-订单状态不允许-返回失败")
    void confirmOrder_InvalidStatus_ReturnsFail() throws Exception {
        doReturn(0).when(jdbcTemplate).update(anyString(), eq("ORD001"));

        mockMvc.perform(put("/api/order/ORD001/confirm")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== PUT /api/order/{orderNo}/cancel ====================

    @Test
    @DisplayName("取消订单-待支付状态-成功")
    void cancelOrder_Pending_Success() throws Exception {
        // 查询订单 - 待支付status=0
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "500.00", 0));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD002"));

        // 查询订单商品
        List<Map<String, Object>> itemRow = List.of(itemRow(100L, 2));
        doReturn(itemRow).when(jdbcTemplate).queryForList(anyString(), eq(1L));

        // 恢复库存
        doReturn(1).when(jdbcTemplate).update(anyString(), eq(2), eq(2), eq(100L));

        // 更新订单状态
        doReturn(1).when(jdbcTemplate).update(eq("UPDATE orders SET status = 4 WHERE order_no = ?"), eq("ORD002"));

        mockMvc.perform(put("/api/order/ORD002/cancel")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("取消订单-已支付状态-不允许取消")
    void cancelOrder_AlreadyPaid_NotAllowed() throws Exception {
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "500.00", 1));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD003"));

        mockMvc.perform(put("/api/order/ORD003/cancel")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请申请退款")));
    }

    // ==================== PUT /api/order/{orderNo}/refund ====================

    @Test
    @DisplayName("申请退款-已支付状态-退款成功")
    void refundOrder_Paid_Success() throws Exception {
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "500.00", 1));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD004"));

        List<Map<String, Object>> itemRow = List.of(itemRow(100L, 1));
        doReturn(itemRow).when(jdbcTemplate).queryForList(anyString(), eq(1L));

        doReturn(1).when(jdbcTemplate).update(anyString(), eq(1), eq(1), eq(100L));
        doReturn(1).when(jdbcTemplate).update(eq("UPDATE orders SET status = 4 WHERE order_no = ?"), eq("ORD004"));

        mockMvc.perform(put("/api/order/ORD004/refund")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"reason":"商品质量问题"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== PUT /api/order/{orderNo}/ship ====================
    // Ship is in whitelist: /api/order/*/ship — no token needed

    @Test
    @DisplayName("发货-已支付订单-发货成功")
    void shipOrder_Paid_Success() throws Exception {
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "500.00", 1));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD005"));
        doReturn(1).when(jdbcTemplate).update(anyString(), eq("ORD005"));

        mockMvc.perform(put("/api/order/ORD005/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"logisticsCompany":"顺丰快递","trackingNo":"SF1234567890"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.logisticsCompany").value("顺丰快递"));
    }

    @Test
    @DisplayName("发货-状态不是已支付-返回错误")
    void shipOrder_NotPaid_ReturnsError() throws Exception {
        List<Map<String, Object>> orderRow = List.of(orderRow(1L, 1L, "500.00", 0));
        doReturn(orderRow).when(jdbcTemplate).queryForList(anyString(), eq("ORD006"));

        mockMvc.perform(put("/api/order/ORD006/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== helpers ====================

    private Map<String, Object> productRow(Long id, String name, int publishStatus, int stock) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("publish_status", publishStatus);
        row.put("stock", stock);
        return row;
    }

    private Map<String, Object> orderRow(Long id, Long userId, String totalAmount, int status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("user_id", userId);
        row.put("total_amount", new BigDecimal(totalAmount));
        row.put("status", status);
        return row;
    }

    private Map<String, Object> itemRow(Long productId, int quantity) {
        Map<String, Object> row = new HashMap<>();
        row.put("product_id", productId);
        row.put("quantity", quantity);
        return row;
    }

    private UserWallet walletWithBalance(String balance) {
        UserWallet w = new UserWallet();
        w.setUserId(1L);
        w.setBalance(new BigDecimal(balance));
        return w;
    }
}
