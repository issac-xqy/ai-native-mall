package org.example.java_ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录实体类
 */
@Data
@TableName("recharge_record")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long walletId;

    /**
     * 充值金额
     */
    private BigDecimal amount;

    /**
     * 充值方式: 1-微信 2-支付宝 3-银行卡 4-余额转账
     */
    private Integer rechargeType;

    /**
     * 充值状态: 0-待支付 1-充值成功 2-充值失败 3-已退款
     */
    private Integer status;

    /**
     * 交易流水号
     */
    private String tradeNo;

    /**
     * 第三方订单号
     */
    private String outTradeNo;

    /**
     * 备注
     */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
