package org.example.java_ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包实体类
 */
@Data
@TableName("user_wallet")
public class UserWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 钱包余额
     */
    private BigDecimal balance;

    /**
     * 累计充值金额
     */
    private BigDecimal totalRecharge;

    /**
     * 累计消费金额
     */
    private BigDecimal totalSpent;

    /**
     * 冻结金额（退款中的金额）
     */
    private BigDecimal frozenAmount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
