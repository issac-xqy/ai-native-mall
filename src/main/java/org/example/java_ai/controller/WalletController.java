package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.entity.RechargeRecord;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.service.UserWalletService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 钱包管理控制器（前台）
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserWalletService walletService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取钱包信息
     */
    @GetMapping("/info")
    public Result<UserWallet> getWalletInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        UserWallet wallet = walletService.getOrCreateWallet(userId);
        return Result.success(wallet);
    }

    /**
     * 获取钱包余额
     */
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        BigDecimal balance = walletService.getBalance(userId);
        return Result.success(balance);
    }

    /**
     * 充值
     */
    @PostMapping("/recharge")
    public Result<RechargeRecord> recharge(
            HttpServletRequest request,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "1") Integer rechargeType,
            @RequestParam(required = false) String remark) {
        
        Long userId = (Long) request.getAttribute("userId");
        
        log.info("用户充值 - userId: {}, amount: {}, type: {}", userId, amount, rechargeType);
        
        // 验证充值金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }
        
        // 限制单次充值金额
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return Result.error("单次充值金额不能超过10000元");
        }
        
        RechargeRecord record = walletService.recharge(userId, amount, rechargeType, remark);
        
        // TODO: 这里应该返回第三方支付链接或二维码
        // 目前简化处理，直接调用确认充值成功
        
        // 模拟支付成功回调
        boolean success = walletService.confirmRechargeSuccess(record.getTradeNo(), "MOCK_" + System.currentTimeMillis());
        
        if (success) {
            return Result.success(record);
        } else {
            return Result.error("充值失败");
        }
    }

    /**
     * 查询充值记录
     */
    @GetMapping("/recharge-records")
    public Result<?> getRechargeRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        
        Long userId = (Long) request.getAttribute("userId");
        
        var page = walletService.getRechargeRecords(userId, pageNum, pageSize, status);
        
        return Result.success(page);
    }

    /**
     * 查询消费记录（已支付订单）
     */
    @GetMapping("/spending-records")
    public Result<?> getSpendingRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Long userId = (Long) request.getAttribute("userId");
        log.info("查询用户消费记录 - userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        // 查询已支付的订单作为消费记录
        String sql = """
            SELECT 
                o.order_no as tradeNo,
                o.total_amount as amount,
                o.create_time as createTime,
                '订单支付' as remark,
                GROUP_CONCAT(oi.product_name SEPARATOR ', ') as products
            FROM orders o
            LEFT JOIN order_item oi ON o.id = oi.order_id
            WHERE o.user_id = ? AND o.status = 1 AND o.deleted = 0
            GROUP BY o.id, o.order_no, o.total_amount, o.create_time
            ORDER BY o.create_time DESC
            LIMIT ? OFFSET ?
            """;
        
        int offset = (pageNum - 1) * pageSize;
        var records = jdbcTemplate.queryForList(sql, userId, pageSize, offset);
        
        // 查询总数
        String countSql = "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = 1 AND deleted = 0";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        
        return Result.success(result);
    }
}
