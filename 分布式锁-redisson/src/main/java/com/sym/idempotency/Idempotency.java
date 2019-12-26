package com.sym.idempotency;

import com.sym.idempotency.service.KeyGenerator;
import com.sym.idempotency.service.impl.DefaultKeyGenerator;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性注解, 表示此接口是幂等性接口, 再调用时会校验判断
 * Created by shenym on 2019/10/22.
 */
@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotency {

    /**
     * 手动指定分布式锁的key
     */
    String key() default "";

    /**
     * 动态生成分布式锁的key
     */
    Class<? extends KeyGenerator> generator() default DefaultKeyGenerator.class;

    /**
     * 锁定时间, 默认不指定任何时间
     */
    long lockTime() default -1;

    /**
     * 锁定时间的单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 如果发生重复提交, 是否允许抛出异常
     *
     * @return true-抛出异常; false-不做处理
     */
    boolean throwIfRepeat() default true;

    /**
     * 如果中间件宕机, 是否允许继续调用
     *
     * @return true-允许继续调用; false-不允许调用, 会抛出异常
     */
    boolean allowIfDown() default true;

}
