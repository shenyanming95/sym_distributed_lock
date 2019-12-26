package com.sym.idempotency.aspect;


import com.sym.idempotency.Idempotency;
import com.sym.idempotency.exception.RepeatedSubmitException;
import com.sym.idempotency.service.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 拦截注解{@link Idempotency}的切面
 * <p>
 * Created by shenym on 2019/10/22.
 */
@Aspect
@Component
public class IdempotencyAspect implements Ordered {

    private final IdempotencyService idempotencyService;

    @Autowired
    public IdempotencyAspect(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * 环绕通知, 对所有标注了{@link Idempotency}注解的接口, 会进行幂等性判断.
     * 如果允许执行则直接调用原方法, 反之抛出{@link RepeatedSubmitException}异常.
     * 具体看接口{@link IdempotencyService}的实现类
     *
     * @param joinPoint   切入点
     * @param idempotency 注解配置
     * @return Object
     */
    @Around("@annotation(idempotency)")
    public Object handler(ProceedingJoinPoint joinPoint, Idempotency idempotency) {
        return idempotencyService.handler(joinPoint, idempotency);
    }


    @Override
    public int getOrder() {
        return -1;
    }
}
