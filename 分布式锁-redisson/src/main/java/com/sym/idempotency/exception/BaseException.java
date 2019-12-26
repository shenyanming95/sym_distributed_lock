package com.sym.idempotency.exception;

/**
 * Created by shenym on 2019/12/26.
 */
public class BaseException extends RuntimeException {
    public BaseException() {
        super();
    }

    public BaseException(String message) {
        super(message);
    }
}
