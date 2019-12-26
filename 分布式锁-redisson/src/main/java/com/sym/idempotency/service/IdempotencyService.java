package com.sym.idempotency.service;

import com.sym.idempotency.Idempotency;
import com.sym.idempotency.exception.RepeatedSubmitException;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 幂等性接口处理器
 * <p>
 * Created by shenym on 2019/10/22.
 */
public interface IdempotencyService {

    /**
     * 通过AOP切面对幂等性接口进行判断
     *
     * @param joinPoint 切入点信息
     * @throws RepeatedSubmitException 重复提交异常
     */
    Object handler(ProceedingJoinPoint joinPoint, Idempotency idempotency) throws RepeatedSubmitException;


}
