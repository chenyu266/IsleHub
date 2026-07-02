package com.islehub.common.exception;

import lombok.Getter;

/**
 * 业务异常类：携带业务错误码的运行时异常，由全局异常处理器统一捕获并转换为标准响应
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }
}
