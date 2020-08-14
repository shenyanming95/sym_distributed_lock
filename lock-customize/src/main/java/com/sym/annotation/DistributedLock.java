package com.sym.annotation;

import com.sym.enums.LockStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 加锁注解
 *
 * @author shenyanming
 * Created on 2020/8/13 16:53
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {
    /**
     * 分布式锁的前缀
     */
    String prefix() default "";

    /**
     * 持有锁的时间.
     * 若为-1, 表示程序运行多久, 就持有锁多久, 会自动延长锁
     */
    int keepTime() default -1;

    /**
     * 等待锁的时间单位
     */
    TimeUnit waitTimeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败的返回信息
     */
    String messageIfLockFail() default "lock fail";

    /**
     * 获取锁失败的降级方法
     */
    String fallbackMethodIfLockFail() default "";

    /**
     * 加锁策略, 支持mysql, redis, zookeeper
     */
    LockStrategy strategy() default LockStrategy.REDIS;
}
