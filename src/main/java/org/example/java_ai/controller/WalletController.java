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
        
        // 返回第三方支付链接（生产环境对接支付网关）
        // 此处简化：直接返回充值记录，支付结果通过回调确认
        return Result.success(record);
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

        var records = walletService.getSpendingRecords(userId, pageNum, pageSize);
        return Result.success(records);
    }
}
