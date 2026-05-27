package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.RechargeRecord;
import org.example.java_ai.entity.UserWallet;
import org.example.java_ai.mapper.RechargeRecordMapper;
import org.example.java_ai.mapper.UserWalletMapper;
import org.example.java_ai.service.UserWalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 钱包服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements UserWalletService {

    private final RechargeRecordMapper rechargeRecordMapper;

    @Override
    public UserWallet getOrCreateWallet(Long userId) {
        log.info("获取或创建用户钱包 - userId: {}", userId);
        
        LambdaQueryWrapper<UserWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserWallet::getUserId, userId);
        
        UserWallet wallet = getOne(wrapper);
        
        if (wallet == null) {
            // 创建新钱包
            wallet = new UserWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setTotalRecharge(BigDecimal.ZERO);
            wallet.setTotalSpent(BigDecimal.ZERO);
            wallet.setFrozenAmount(BigDecimal.ZERO);
            
            save(wallet);
            log.info("✅ 创建用户钱包成功 - walletId: {}", wallet.getId());
        }
        
        return wallet;
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        return wallet.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord recharge(Long userId, BigDecimal amount, Integer rechargeType, String remark) {
        log.info("用户充值 - userId: {}, amount: {}, type: {}", userId, amount, rechargeType);
        
        // 1. 获取或创建钱包
        UserWallet wallet = getOrCreateWallet(userId);
        
        // 2. 生成交易流水号
        String tradeNo = "R" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // 3. 创建充值记录
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setWalletId(wallet.getId());
        record.setAmount(amount);
        record.setRechargeType(rechargeType != null ? rechargeType : 1);
        record.setStatus(0); // 待支付
        record.setTradeNo(tradeNo);
        record.setRemark(remark);
        
        rechargeRecordMapper.insert(record);
        
        log.info("✅ 充值记录创建成功 - tradeNo: {}, recordId: {}, walletId: {}", tradeNo, record.getId(), wallet.getId());
        
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmRechargeSuccess(String tradeNo, String outTradeNo) {
        log.info("确认充值成功 - tradeNo: {}, outTradeNo: {}", tradeNo, outTradeNo);
        
        // 1. 查找充值记录
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getTradeNo, tradeNo);
        
        RechargeRecord record = rechargeRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            log.error("充值记录不存在 - tradeNo: {}", tradeNo);
            return false;
        }
        
        if (record.getStatus() != 0) {
            log.warn("充值记录状态异常 - tradeNo: {}, status: {}", tradeNo, record.getStatus());
            return false;
        }
        
        // 2. 更新充值记录状态
        record.setStatus(1); // 充值成功
        record.setOutTradeNo(outTradeNo);
        record.setUpdateTime(LocalDateTime.now());
        rechargeRecordMapper.updateById(record);
        
        // 3. 更新钱包余额
        UserWallet wallet = getOrCreateWallet(record.getUserId());
        wallet.setBalance(wallet.getBalance().add(record.getAmount()));
        wallet.setTotalRecharge(wallet.getTotalRecharge().add(record.getAmount()));
        wallet.setUpdateTime(LocalDateTime.now());
        updateById(wallet);
        
        log.info("✅ 充值成功 - userId: {}, amount: {}, newBalance: {}", 
                wallet.getUserId(), record.getAmount(), wallet.getBalance());
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount, String remark) {
        log.info("消费扣款 - userId: {}, amount: {}", userId, amount);
        
        UserWallet wallet = getOrCreateWallet(userId);
        
        // 检查余额是否充足
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("余额不足 - userId: {}, balance: {}, amount: {}", userId, wallet.getBalance(), amount);
            return false;
        }
        
        // 扣款
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalSpent(wallet.getTotalSpent().add(amount));
        wallet.setUpdateTime(LocalDateTime.now());
        
        boolean success = updateById(wallet);
        
        if (success) {
            log.info("✅ 扣款成功 - userId: {}, amount: {}, newBalance: {}", userId, amount, wallet.getBalance());
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(Long userId, BigDecimal amount, String remark) {
        log.info("退款 - userId: {}, amount: {}", userId, amount);
        
        UserWallet wallet = getOrCreateWallet(userId);
        
        // 退款到余额
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdateTime(LocalDateTime.now());
        
        boolean success = updateById(wallet);
        
        if (success) {
            log.info("✅ 退款成功 - userId: {}, amount: {}, newBalance: {}", userId, amount, wallet.getBalance());
        }
        
        return success;
    }

    @Override
    public Page<RechargeRecord> getRechargeRecords(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        log.info("查询充值记录 - userId: {}, pageNum: {}, pageSize: {}, status: {}", userId, pageNum, pageSize, status);
        
        Page<RechargeRecord> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getUserId, userId);
        
        if (status != null) {
            wrapper.eq(RechargeRecord::getStatus, status);
        }
        
        wrapper.orderByDesc(RechargeRecord::getCreateTime);
        
        return rechargeRecordMapper.selectPage(page, wrapper);
    }
}
