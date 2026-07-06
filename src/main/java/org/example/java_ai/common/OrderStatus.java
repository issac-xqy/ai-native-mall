package org.example.java_ai.common;

/** 订单状态枚举 */
public enum OrderStatus {
    CREATED(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid order status: " + code);
    }

    /** 检查是否可以转换到此状态 */
    public static boolean canTransition(int from, int to) {
        return switch (from) {
            case 0 -> to == 1 || to == 4;
            case 1 -> to == 2;
            case 2 -> to == 3;
            case 3, 4 -> false;
            default -> false;
        };
    }
}
