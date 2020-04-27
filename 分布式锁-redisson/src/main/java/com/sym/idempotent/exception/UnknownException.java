package com.sym.idempotent.exception;

/**
 * 未知异常
 *
 * @author shenyanming
 */
public class UnknownException extends BaseIdempotentException {

    public UnknownException(){
        super();
    }

    public UnknownException(String message){
        super(message);
    }

    public UnknownException(Throwable cause) {
        super(cause);
    }
}
