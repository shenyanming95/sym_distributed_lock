package com.sym.idempotent.service;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 分布式锁Key生成器
 *
 * @author shenym
 * @date 2019/12/26
 */
public interface KeyGenerator {
    /**
     * 获取key
     */
    String generate(ProceedingJoinPoint joinPoint);
}
