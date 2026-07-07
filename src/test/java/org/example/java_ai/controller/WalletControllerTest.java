package org.example.java_ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.java_ai.entity.RechargeRecord;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.service.UserWalletService;
import org.example.java_ai.util.TokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = WalletController.class,
        excludeAutoConfiguration = {
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@DisplayName("WalletController MockMvc 测试")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserWalletService walletService;

    private final String token = TokenUtil.generateAccessToken(1L);

    @Test
    @DisplayName("钱包信息-有钱包-返回钱包数据")
    void getWalletInfo_HasWallet_ReturnsWallet() throws Exception {
        UserWallet wallet = buildWallet(1L, 1L, "5000.00");
        when(walletService.getOrCreateWallet(1L)).thenReturn(wallet);

        mockMvc.perform(get("/wallet/info")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(5000))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("钱包余额-返回余额")
    void getBalance_HasBalance_ReturnsBalance() throws Exception {
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("9999.99"));

        mockMvc.perform(get("/wallet/balance")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(9999.99));
    }

    @Test
    @DisplayName("充值-正常金额-成功")
    void recharge_ValidAmount_Success() throws Exception {
        RechargeRecord record = buildRechargeRecord(1L, "500.00", "R001", 0);
        when(walletService.recharge(eq(1L), any(BigDecimal.class), eq(1), any())).thenReturn(record);

        mockMvc.perform(post("/wallet/recharge")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .param("amount", "500.00")
                        .param("rechargeType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("充值-金额为0-返回错误")
    void recharge_ZeroAmount_ReturnsError() throws Exception {
        mockMvc.perform(post("/wallet/recharge")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("充值-负数金额-返回错误")
    void recharge_NegativeAmount_ReturnsError() throws Exception {
        mockMvc.perform(post("/wallet/recharge")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .param("amount", "-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("充值-超过10000-返回错误")
    void recharge_ExceedLimit_ReturnsError() throws Exception {
        mockMvc.perform(post("/wallet/recharge")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .param("amount", "10000.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("充值记录-分页查询-返回分页")
    void getRechargeRecords_Paginated_ReturnsPage() throws Exception {
        Page<RechargeRecord> page = new Page<>(1, 10);
        page.setTotal(2);
        when(walletService.getRechargeRecords(eq(1L), eq(1), eq(10), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/wallet/recharge-records")
                        .requestAttr("userId", 1L)
                        .header("Authorization", token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("无Token访问钱包-返回200(Controller层不做null检查)")
    void noToken_Returns200() throws Exception {
        when(walletService.getOrCreateWallet(null)).thenReturn(buildWallet(null, null, "0.00"));
        mockMvc.perform(get("/wallet/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private UserWallet buildWallet(Long id, Long userId, String balance) {
        UserWallet w = new UserWallet();
        w.setId(id);
        w.setUserId(userId);
        w.setBalance(new BigDecimal(balance));
        w.setTotalRecharge(BigDecimal.ZERO);
        w.setTotalSpent(BigDecimal.ZERO);
        return w;
    }

    private RechargeRecord buildRechargeRecord(Long userId, String amount, String tradeNo, int status) {
        RechargeRecord r = new RechargeRecord();
        r.setId(1L);
        r.setUserId(userId);
        r.setAmount(new BigDecimal(amount));
        r.setTradeNo(tradeNo);
        r.setStatus(status);
        return r;
    }
}
