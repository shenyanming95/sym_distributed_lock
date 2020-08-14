package com.sym.idempotent.exception;

/**
 * 回调原方法异常
 * @author shenyanming
 */
public class InvokeException extends BaseIdempotentException {

    public InvokeException() {
        super();
    }

    public InvokeException(String message) {
        super(message);
    }

    public InvokeException(Throwable cause) {
        super(cause);
    }
}
