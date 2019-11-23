package com.sym.lock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by shenym on 2019/11/23 13:40.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SymRedissonLock {

    private final static String LOCK_PREFIX = "redisson:lock:";

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 普通锁
     */
    @Test
    public void testOne(){
        RLock lock = redissonClient.getLock(LOCK_PREFIX + 1);
        boolean b = lock.tryLock();
        System.out.println(b);
    }

    /**
     * 读写锁
     */
    @Test
    public void testTwo(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(LOCK_PREFIX + 2);
        RLock readLock = readWriteLock.readLock();
        RLock writeLock = readWriteLock.writeLock();
        boolean b = readLock.tryLock();
        boolean c = writeLock.tryLock();
        System.out.println(b);
        System.out.println(c);
    }
}
