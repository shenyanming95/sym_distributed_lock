package com.sym.idempotency.service.impl;


import com.sym.idempotency.Idempotency;
import com.sym.idempotency.exception.IdempotencyException;
import com.sym.idempotency.exception.RepeatedSubmitException;
import com.sym.idempotency.exception.UnknownException;
import com.sym.idempotency.service.IdempotencyService;
import com.sym.idempotency.service.KeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.WriteRedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 幂等性接口实现类
 *
 * @see IdempotencyService
 * @see RedissonClient
 * <p>
 * Created by shenym on 2019/10/24.
 */
@Service("idempotencyService")
public class IdempotencyServiceImpl implements IdempotencyService {

    private final static Logger logger = LoggerFactory.getLogger(IdempotencyServiceImpl.class);
    private final static Object LOCK_OBJECT = new Object();
    private static Map<Class<? extends KeyGenerator>, KeyGenerator> keyGeneratorMap = new HashMap<>(64);

    private final RedissonClient redissonClient;

    @Autowired
    public IdempotencyServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Object handler(ProceedingJoinPoint joinPoint, Idempotency idempotency) throws RepeatedSubmitException {
        // 获取分布式锁的key
        String key = this.getKey(joinPoint, idempotency);
        // 获取注解的配置
        long lockTime = idempotency.lockTime();
        boolean allowIfDown = idempotency.allowIfDown();
        boolean throwIfRepeat = idempotency.throwIfRepeat();
        // 获取分布式锁对象
        RLock lock = redissonClient.getLock(key);
        try {
            boolean isGetLock;
            if (lockTime > 0) {
                // 如果 lockTime 大于 0, 则该锁只会锁住指定的时间
                isGetLock = lock.tryLock(0, lockTime, idempotency.timeUnit());
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
            logger.warn("[幂等性接口]发生重复提交：{}", joinPoint.getSignature().toShortString());
            throw e;
        } catch (WriteRedisConnectionException e) {
            // redis宕机, 或者网络异常连接不上.
            if (allowIfDown) {
                logger.warn("[幂等性接口]redis连接失败, 但允许访问幂等接口");
                return invoke(joinPoint);
            } else {
                logger.error("[幂等性接口]redis连接失败, 原因：{}", e.getMessage());
                throw e;
            }
        } catch (InterruptedException e) {
            logger.error("[幂等性接口]线程被中断");
            throw new IdempotencyException();
        } catch (Exception e) {
            logger.error("[幂等性接口]发生未知异常, 原因：{}", e.getMessage());
            throw new IdempotencyException();
        }
    }

    private String getKey(ProceedingJoinPoint joinPoint, Idempotency idempotency) {
        String customizedKey = idempotency.key();
        // 优先取手动设置的key
        if (!StringUtils.isEmpty(customizedKey)) {
            return customizedKey;
        }
        Class<? extends KeyGenerator> keyGeneratorClass = idempotency.generator();
        KeyGenerator keyGenerator = keyGeneratorMap.get(keyGeneratorClass);
        return null == keyGenerator ? doGetKey(joinPoint, keyGeneratorClass) : keyGenerator.generate(joinPoint);
    }

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
                    throw new UnknownException(e.getMessage());
                }
            }
            return oldKeyGenerator.generate(joinPoint);
        }
    }

    private Object invoke(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable e) {
            throw new UnknownException(e.getMessage());
        }
    }

}
