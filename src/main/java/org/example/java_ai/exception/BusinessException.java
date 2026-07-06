package org.example.java_ai.exception;

import lombok.Getter;
import org.example.java_ai.common.ResultCode;

/**
 * 业务异常基类
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 限流场景：重试等待秒数
     */
    private final Integer retryAfterSeconds;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.message = message;
        this.retryAfterSeconds = null;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.retryAfterSeconds = null;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.retryAfterSeconds = null;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
        this.retryAfterSeconds = null;
    }

    public BusinessException(ResultCode resultCode, String message, Integer retryAfterSeconds) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
