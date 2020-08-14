package com.sym.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 结合{@link DistributedLock}组合成分布式锁的key.
 * 如果需要多个参数组合, 通过指定position可以按顺序拼在一起.
 *
 * @author shenyanming
 * Created on 2020/8/13 17:08
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface LockPrefixName {
    /**
     * 多个{@link LockPrefixName}, 根据该值(越大越优先)拼接
     * 分布式锁的key
     */
    int position() default 0;
}
