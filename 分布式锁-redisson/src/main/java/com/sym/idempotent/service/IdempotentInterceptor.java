package com.sym.idempotent.service;

import com.sym.idempotent.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 幂等性接口处理器
 *
 * @author shenym
 * @date 2019/10/22
 */
public interface IdempotentInterceptor {

    /**
     * 拦截要执行的方法, 判断它是否有重复提交的情况
     *
     * @param joinPoint 方法签名
     * @param idempotent 注解信息
     * @return 原方法返回值
     * @exception Exception 异步发生
     */
    Object determine(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Exception;


}
