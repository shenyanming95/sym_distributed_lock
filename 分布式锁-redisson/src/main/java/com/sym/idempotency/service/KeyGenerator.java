package com.sym.idempotency.service;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 分布式锁Key生成器
 *
 * Created by shenym on 2019/12/26.
 */
public interface KeyGenerator {

    /**
     * 获取key
     */
    String generate(ProceedingJoinPoint joinPoint);
}
