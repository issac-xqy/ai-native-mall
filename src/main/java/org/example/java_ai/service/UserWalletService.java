package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.entity.RechargeRecord;
import org.example.java_ai.entity.UserWallet;

import java.math.BigDecimal;

/**
 * 钱包服务接口
 */
public interface UserWalletService extends IService<UserWallet> {

    /**
     * 获取或创建用户钱包
     */
    UserWallet getOrCreateWallet(Long userId);

    /**
     * 查询钱包余额
     */
    BigDecimal getBalance(Long userId);

    /**
     * 用户充值
     */
    RechargeRecord recharge(Long userId, BigDecimal amount, Integer rechargeType, String remark);

    /**
     * 确认充值成功（支付回调）
     */
    boolean confirmRechargeSuccess(String tradeNo, String outTradeNo);

    /**
     * 消费扣款
     */
    boolean deductBalance(Long userId, BigDecimal amount, String remark);

    /**
     * 退款
     */
    boolean refund(Long userId, BigDecimal amount, String remark);

    /**
     * 分页查询充值记录
     */
    Page<RechargeRecord> getRechargeRecords(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查询消费记录
     */
    java.util.Map<String, Object> getSpendingRecords(Long userId, Integer pageNum, Integer pageSize);
}
