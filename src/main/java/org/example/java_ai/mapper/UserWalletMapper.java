package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.java_ai.entity.UserWallet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包Mapper
 */
@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {

    /**
     * 原子扣款：检查余额 >= amount 后扣减
     * @return 影响行数，0表示余额不足
     */
    @Update("UPDATE user_wallet SET balance = balance - #{amount}, " +
            "total_spent = total_spent + #{amount}, " +
            "update_time = #{now} " +
            "WHERE user_id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount,
                      @Param("now") LocalDateTime now);

    /**
     * 原子退款
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, " +
            "update_time = #{now} " +
            "WHERE user_id = #{userId}")
    int refund(@Param("userId") Long userId, @Param("amount") BigDecimal amount,
               @Param("now") LocalDateTime now);
}
