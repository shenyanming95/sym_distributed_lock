package com.sym.idempotency.exception;



/**
 * 幂等操作的未知异常
 *
 * Created by shenym on 2019/12/26.
 */
public class UnknownException extends BaseException {

    public UnknownException(){
        super();
    }

    public UnknownException(String message){
        super(message);
    }

}
