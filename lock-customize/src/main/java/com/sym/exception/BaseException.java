package com.sym.exception;

import java.io.Serializable;

/**
 * 分布式锁, 基础异常
 *
 * @author shenyanming
 * Created on 2020/6/11 17:57
 */
public class BaseException extends RuntimeException implements Serializable {

    private int errorCode;
    private String errorMessage;

    public BaseException() {
        this(-1, "");
    }

    public BaseException(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
