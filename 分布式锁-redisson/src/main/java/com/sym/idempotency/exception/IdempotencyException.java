package com.sym.idempotency.exception;


/**
 * 幂等性异常
 * <p>
 * Created by shenym on 2019/10/22.
 */
public class IdempotencyException extends BaseException {

    public IdempotencyException() {
        super();
    }

    public IdempotencyException(String message) {
        super(message);
    }

}
