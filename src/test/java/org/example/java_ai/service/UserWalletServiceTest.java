package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.java_ai.entity.RechargeRecord;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.mapper.RechargeRecordMapper;
import org.example.java_ai.mapper.UserWalletMapper;
import org.example.java_ai.service.impl.UserWalletServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserWalletService 单元测试")
class UserWalletServiceTest {

    @Mock
    private UserWalletMapper walletMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    private UserWalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        walletService = new UserWalletServiceImpl(rechargeRecordMapper);
        ReflectionTestUtils.setField(walletService, "baseMapper", walletMapper);
    }

    @Test
    @DisplayName("getOrCreateWallet-钱包已存在-直接返回")
    void getOrCreateWallet_Exists_ReturnsExisting() {
        UserWallet existing = buildWallet(1L, 1L, "500.00");
        doReturn(existing).when(walletMapper).selectOne(any(), anyBoolean());

        UserWallet result = walletService.getOrCreateWallet(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getBalance()));
    }

    @Test
    @DisplayName("getOrCreateWallet-钱包不存在-创建新钱包")
    void getOrCreateWallet_NotExists_CreatesNew() {
        doReturn(null).when(walletMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(walletMapper).insert((UserWallet) any());

        UserWallet result = walletService.getOrCreateWallet(2L);

        assertNotNull(result);
        assertEquals(2L, result.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalance()));
    }

    @Test
    @DisplayName("getBalance-有钱包-返回余额")
    void getBalance_HasWallet_ReturnsBalance() {
        doReturn(buildWallet(1L, 1L, "999.99")).when(walletMapper).selectOne(any(), anyBoolean());

        BigDecimal balance = walletService.getBalance(1L);

        assertEquals(0, new BigDecimal("999.99").compareTo(balance));
    }

    @Test
    @DisplayName("recharge-正常充值-创建待支付记录")
    void recharge_Normal_CreatesPendingRecord() {
        doReturn(buildWallet(1L, 1L, "100.00")).when(walletMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(rechargeRecordMapper).insert((RechargeRecord) any());

        RechargeRecord record = walletService.recharge(1L, new BigDecimal("200.00"), 1, "充值");

        assertNotNull(record);
        assertEquals(1L, record.getUserId());
        assertEquals(0, new BigDecimal("200.00").compareTo(record.getAmount()));
        assertEquals(0, record.getStatus());
        assertNotNull(record.getTradeNo());
    }

    @Test
    @DisplayName("confirmRechargeSuccess-正常-更新记录和余额")
    void confirmRechargeSuccess_Normal_UpdatesBoth() {
        RechargeRecord record = buildRecord(1L, 1L, "200.00", "R001", 0);
        doReturn(record).when(rechargeRecordMapper).selectOne(any());
        doReturn(1).when(rechargeRecordMapper).updateById((RechargeRecord) any());
        doReturn(buildWallet(2L, 1L, "100.00")).when(walletMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(walletMapper).updateById((UserWallet) any());

        assertTrue(walletService.confirmRechargeSuccess("R001", "OUT_001"));
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("confirmRechargeSuccess-记录不存在-返回false")
    void confirmRechargeSuccess_RecordNotFound_ReturnsFalse() {
        doReturn(null).when(rechargeRecordMapper).selectOne(any());

        assertFalse(walletService.confirmRechargeSuccess("NOT_EXIST", "OUT_001"));
    }

    @Test
    @DisplayName("confirmRechargeSuccess-已处理记录-返回false")
    void confirmRechargeSuccess_AlreadyProcessed_ReturnsFalse() {
        doReturn(buildRecord(1L, 1L, "200.00", "R001", 1))
                .when(rechargeRecordMapper).selectOne(any());

        assertFalse(walletService.confirmRechargeSuccess("R001", "OUT_002"));
    }

    @Test
    @DisplayName("deductBalance-余额充足-扣款成功")
    void deductBalance_Sufficient_ReturnsTrue() {
        doReturn(buildWallet(1L, 1L, "500.00")).when(walletMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(walletMapper).updateById((UserWallet) any());

        assertTrue(walletService.deductBalance(1L, new BigDecimal("300.00"), "购买商品"));
    }

    @Test
    @DisplayName("deductBalance-余额不足-返回false")
    void deductBalance_Insufficient_ReturnsFalse() {
        doReturn(buildWallet(1L, 1L, "100.00")).when(walletMapper).selectOne(any(), anyBoolean());

        assertFalse(walletService.deductBalance(1L, new BigDecimal("500.00"), "买不起"));
    }

    @Test
    @DisplayName("refund-正常退款-余额增加")
    void refund_Normal_IncreasesBalance() {
        doReturn(buildWallet(1L, 1L, "1000.00")).when(walletMapper).selectOne(any(), anyBoolean());
        doReturn(1).when(walletMapper).updateById((UserWallet) any());

        assertTrue(walletService.refund(1L, new BigDecimal("200.00"), "订单退款"));
    }

    @Test
    @DisplayName("getRechargeRecords-分页查询-返回分页")
    void getRechargeRecords_Paginated_ReturnsPage() {
        Page<RechargeRecord> mockPage = new Page<>(1, 10);
        mockPage.setTotal(3);
        doReturn(mockPage).when(rechargeRecordMapper).selectPage(any(Page.class), any());

        Page<RechargeRecord> result = walletService.getRechargeRecords(1L, 1, 10, null);

        assertNotNull(result);
        assertEquals(3, result.getTotal());
    }

    private UserWallet buildWallet(Long id, Long userId, String balance) {
        UserWallet w = new UserWallet();
        w.setId(id);
        w.setUserId(userId);
        w.setBalance(new BigDecimal(balance));
        w.setTotalRecharge(BigDecimal.ZERO);
        w.setTotalSpent(BigDecimal.ZERO);
        w.setFrozenAmount(BigDecimal.ZERO);
        return w;
    }

    private RechargeRecord buildRecord(Long id, Long userId, String amount, String tradeNo, int status) {
        RechargeRecord r = new RechargeRecord();
        r.setId(id);
        r.setUserId(userId);
        r.setWalletId(1L);
        r.setAmount(new BigDecimal(amount));
        r.setTradeNo(tradeNo);
        r.setStatus(status);
        return r;
    }
}
