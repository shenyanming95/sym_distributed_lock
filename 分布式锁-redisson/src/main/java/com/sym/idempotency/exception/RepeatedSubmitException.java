package com.sym.idempotency.exception;


/**
 * 重复提交异常
 * <p>
 * Created by shenym on 2019/10/22.
 */
public class RepeatedSubmitException extends BaseException {

    public RepeatedSubmitException() {
        super();
    }

    public RepeatedSubmitException(String message) {
        super(message);
    }

}
