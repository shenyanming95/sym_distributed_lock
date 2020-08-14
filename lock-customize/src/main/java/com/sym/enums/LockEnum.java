package com.sym.enums;

/**
 * @author shenyanming
 * Created on 2020/8/13 17:22
 */
public enum LockEnum {

    METHOD_WITHOUT_ANNOTATION_LOCK(1001, "方法没有标注DistributedLock注解");

    private int code;
    private String name;

    LockEnum(int code, String name){
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
