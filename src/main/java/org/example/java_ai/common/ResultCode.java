package org.example.java_ai.common;

import lombok.Getter;

/**
 * 响应码枚举
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Getter
public enum ResultCode {
    
    // 成功
    SUCCESS(200, "操作成功"),
    
    // 客户端错误 (4xx)
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    
    // 服务端错误 (5xx)
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // 业务错误 (1xxx)
    BUSINESS_ERROR(1000, "业务处理失败"),
    VALIDATION_ERROR(1001, "数据验证失败"),
    DUPLICATE_SUBMIT(1002, "重复提交"),
    INSUFFICIENT_STOCK(1003, "库存不足"),
    ORDER_NOT_FOUND(1004, "订单不存在"),
    PRODUCT_NOT_FOUND(1005, "商品不存在"),
    USER_NOT_FOUND(1006, "用户不存在"),
    
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),
    LOGIN_RATE_LIMITED(4001, "登录尝试过于频繁"),

    // AI相关错误 (2xxx)
    AI_SERVICE_ERROR(2000, "AI服务异常"),
    AI_TIMEOUT(2001, "AI服务超时"),
    AI_RATE_LIMIT(2002, "AI接口调用频率超限"),
    RAG_RETRIEVAL_FAILED(2003, "知识库检索失败");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
