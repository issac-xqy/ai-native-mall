package org.example.java_ai.util;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据脱敏工具类
 * 
 * 遵循《个人信息保护法》要求，对用户隐私数据进行脱敏处理
 * 
 * 脱敏规则：
 * 1. 手机号：138****5678（中间4位）
 * 2. 身份证：110**********1234（中间9位）
 * 3. 姓名：张*（只显示姓）
 * 4. 邮箱：z***@gmail.com（用户名部分脱敏）
 * 5. 地址：北京市朝阳区****（详细地址脱敏）
 * 6. 银行卡：6222 **** **** 1234（只保留前后4位）
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
public class DataMaskingUtil {

    /**
     * 手机号脱敏
     * 示例：13812345678 → 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 身份证号脱敏
     * 示例：110101199001011234 → 110**********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return DesensitizedUtil.idCardNum(idCard, 3, 4);
    }

    /**
     * 姓名脱敏
     * 示例：张三 → 张*
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return DesensitizedUtil.chineseName(name);
    }

    /**
     * 邮箱脱敏
     * 示例：zhangsan@gmail.com → z***@gmail.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 地址脱敏
     * 示例：北京市朝阳区建国路123号 → 北京市朝阳区****
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() < 6) {
            return address;
        }
        // 保留前6个字符（省市区），其余用****替代
        return address.substring(0, 6) + "****";
    }

    /**
     * 银行卡号脱敏
     * 示例：6222021234567890123 → 6222 **** **** 0123
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return DesensitizedUtil.bankCard(bankCard);
    }

    /**
     * 密码脱敏（完全隐藏）
     */
    public static String maskPassword(String password) {
        return "******";
    }

    /**
     * IP地址脱敏
     * 示例：192.168.1.100 → 192.168.1.*
     */
    public static String maskIp(String ip) {
        if (ip == null || !ip.contains(".")) {
            return ip;
        }
        int lastDotIndex = ip.lastIndexOf(".");
        return ip.substring(0, lastDotIndex + 1) + "*";
    }

    /**
     * 用户信息综合脱敏（用于日志输出）
     */
    public static String maskUserInfo(String name, String phone, String address) {
        return String.format("用户[%s, %s, %s]", 
            maskName(name), 
            maskPhone(phone), 
            maskAddress(address)
        );
    }

    /**
     * 订单信息脱敏（用于日志输出）
     */
    public static String maskOrderInfo(String orderId, String userName, String userPhone, String amount) {
        return String.format("订单[ID:%s, 用户:%s, 手机:%s, 金额:%s元]", 
            orderId,
            maskName(userName), 
            maskPhone(userPhone),
            amount
        );
    }
}
