package org.example.java_ai.util;

import cn.hutool.core.util.DesensitizedUtil;

/**
 * 数据脱敏工具类 — 日志输出隐私保护
 */
public class DataMaskingUtil {

    private DataMaskingUtil() {}

    /** 手机号脱敏: 13812345678 → 138****5678 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return DesensitizedUtil.mobilePhone(phone);
    }

    /** 姓名脱敏: 张三 → 张* */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        return DesensitizedUtil.chineseName(name);
    }

    /** 邮箱脱敏: zhangsan@gmail.com → z***@gmail.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        return DesensitizedUtil.email(email);
    }

    /** 用户信息综合脱敏 (用于日志输出) */
    public static String maskUserInfo(String name, String phone, String address) {
        return String.format("用户[%s, %s, %s]",
            maskName(name), maskPhone(phone), address != null && address.length() >= 6
                ? address.substring(0, 6) + "****" : address);
    }
}
