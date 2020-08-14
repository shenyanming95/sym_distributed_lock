package com.sym.exception;

import com.sym.enums.LockEnum;

/**
 * @author shenyanming
 * Created on 2020/8/13 17:24
 */
public class ReflectionException extends BaseException {

    public ReflectionException(LockEnum lockEnum){
        super(lockEnum.getCode(), lockEnum.getName());
    }
}
