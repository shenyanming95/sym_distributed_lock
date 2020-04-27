package com.sym.idempotent.service.impl;

import com.sym.idempotent.Idempotent;
import com.sym.idempotent.exception.InvokeException;
import com.sym.idempotent.exception.RepeatedSubmitException;
import com.sym.idempotent.exception.UnknownException;
import com.sym.idempotent.service.IdempotentInterceptor;
import com.sym.idempotent.service.KeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.WriteRedisConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 幂等性接口实现类
 *
 * @author shenyanming
 * @see IdempotentInterceptor
 * @see RedissonClient
 * Created by shenym on 2019/10/24.
 */
@Component
@Slf4j
public class DefaultIdempotentInterceptor implements IdempotentInterceptor {

    private final static Object LOCK_OBJECT = new Object();
    private static Map<Class<? extends KeyGenerator>, KeyGenerator> keyGeneratorMap = new HashMap<>(64);
    private final RedissonClient redissonClient;

    @Autowired
    public DefaultIdempotentInterceptor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Object determine(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws RepeatedSubmitException {
        // 获取分布式锁的key
        String key = this.getKey(joinPoint, idempotent);
        // 获取注解的配置
        long lockTime = idempotent.lockTime();
        boolean allowIfDown = idempotent.allowIfDown();
        boolean throwIfRepeat = idempotent.throwIfRepeat();
        // 获取分布式锁对象
        RLock lock = redissonClient.getLock(key);
        try {
            boolean isGetLock;
            if (lockTime > 0) {
                // 如果 lockTime 大于 0, 则该锁只会锁住指定的时间
                isGetLock = lock.tryLock(0, lockTime, idempotent.timeUnit());
            } else {
                // 否则会一直锁住(Redisson默认锁住50s并且在线程运行期间有看门狗不断延长此时间), 直至方法调用完成
                isGetLock = lock.tryLock();
            }
            if (isGetLock) {
                try {
                    // 能够获取到锁, 说明此次请求是首次提交, 回调原有方法
                    return invoke(joinPoint);
                } finally {
                    // 解锁
                    lock.unlock();
                }
            } else {
                if (throwIfRepeat) {
                    // 如果获取锁失败, 说明已经有一次请求正在处理, 此次请求就属于重复提交, 抛出异常
                    throw new RepeatedSubmitException();
                } else {
                    return null;
                }
            }
        } catch (RepeatedSubmitException e) {
            log.warn("发生重复提交：{}", joinPoint.getSignature().toShortString());
            throw e;
        } catch (WriteRedisConnectionException e) {
            // redis宕机, 或者网络异常连接不上.
            if (allowIfDown) {
                log.warn("redis连接失败, 但允许访问幂等接口");
                return invoke(joinPoint);
            } else {
                log.error("redis连接失败, 原因：{}", e);
                throw e;
            }
        } catch (Exception e) {
            log.error("发生未知异常, 原因：{}", e);
            throw new UnknownException(e);
        }
    }

    /**
     * 获取key
     */
    private String getKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String customizedKey = idempotent.key();
        // 优先取手动设置的key
        if (!StringUtils.isEmpty(customizedKey)) {
            return customizedKey;
        }
        Class<? extends KeyGenerator> keyGeneratorClass = idempotent.generator();
        KeyGenerator keyGenerator = keyGeneratorMap.get(keyGeneratorClass);
        return null == keyGenerator ? doGetKey(joinPoint, keyGeneratorClass) : keyGenerator.generate(joinPoint);
    }

    /**
     * 生成key
     */
    private String doGetKey(ProceedingJoinPoint joinPoint, Class<? extends KeyGenerator> keyGeneratorClass) {
        synchronized (LOCK_OBJECT) {
            KeyGenerator oldKeyGenerator = keyGeneratorMap.get(keyGeneratorClass);
            if (null == oldKeyGenerator) {
                try {
                    KeyGenerator keyGenerator = keyGeneratorClass.newInstance();
                    String newKey = keyGenerator.generate(joinPoint);
                    keyGeneratorMap.put(keyGeneratorClass, keyGenerator);
                    return newKey;
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("生成KeyGenerator实例失败: {}", e);
                    throw new InvokeException(e);
                }
            }
            return oldKeyGenerator.generate(joinPoint);
        }
    }

    /**
     * 回调目标方法
     */
    private Object invoke(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable e) {
            log.error("回调目标方法失败: {}", e);
            throw new InvokeException(e);
        }
    }

}
