package com.sym.idempotent.exception;

/**
 * 幂等性判断的异常基础类
 *
 * @author shenym
 * @date 2019/12/26
 */
public class BaseIdempotentException extends RuntimeException {

    // 省略一些错误码..

    public BaseIdempotentException() {
        super();
    }

    public BaseIdempotentException(String message) {
        super(message);
    }

    public BaseIdempotentException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseIdempotentException(Throwable cause) {
        super(cause);
    }

    protected BaseIdempotentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
