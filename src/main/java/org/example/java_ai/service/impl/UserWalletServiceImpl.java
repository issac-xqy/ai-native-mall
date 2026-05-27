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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

        // 原子更新充值记录状态（防止重复入账）
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getTradeNo, tradeNo)
               .eq(RechargeRecord::getStatus, 0);

        RechargeRecord record = rechargeRecordMapper.selectOne(wrapper);
        if (record == null) {
            log.warn("充值记录不存在或已处理 - tradeNo: {}", tradeNo);
            return false;
        }

        record.setStatus(1);
        record.setOutTradeNo(outTradeNo);
        record.setUpdateTime(LocalDateTime.now());
        rechargeRecordMapper.updateById(record);

        // 原子增加余额（无并发超扣风险）
        getOrCreateWallet(record.getUserId());
        baseMapper.refund(record.getUserId(), record.getAmount(), LocalDateTime.now());

        log.info("充值成功 - userId: {}, amount: {}", record.getUserId(), record.getAmount());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount, String remark) {
        log.info("消费扣款 - userId: {}, amount: {}", userId, amount);
        int affected = baseMapper.deductBalance(userId, amount, LocalDateTime.now());
        if (affected > 0) {
            log.info("扣款成功 - userId: {}, amount: {}", userId, amount);
            return true;
        }
        log.warn("余额不足或扣款失败 - userId: {}, amount: {}", userId, amount);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(Long userId, BigDecimal amount, String remark) {
        log.info("退款 - userId: {}, amount: {}", userId, amount);
        getOrCreateWallet(userId);
        int affected = baseMapper.refund(userId, amount, LocalDateTime.now());
        if (affected > 0) {
            log.info("退款成功 - userId: {}, amount: {}", userId, amount);
            return true;
        }
        return false;
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

    @Override
    public java.util.Map<String, Object> getSpendingRecords(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
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
        var records = jdbcTemplate.queryForList(sql, userId, pageSize, offset);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = 1 AND deleted = 0",
                Long.class, userId);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        return result;
    }
}
