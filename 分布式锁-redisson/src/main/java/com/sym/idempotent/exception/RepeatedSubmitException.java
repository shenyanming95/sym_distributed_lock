package com.sym.idempotent.exception;


/**
 * 重复提交异常
 *
 * @author shenym
 * @date 2019/10/22
 */
public class RepeatedSubmitException extends BaseIdempotentException {

    public RepeatedSubmitException() {
        super();
    }

    public RepeatedSubmitException(String message) {
        super(message);
    }

}
