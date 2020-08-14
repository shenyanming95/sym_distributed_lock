package com.sym.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口
 *
 * @author shenyanming
 * Created on 2020/4/28 17:38
 */
public interface ILock {

    /**
     * 获取分布式锁, 抢夺失败会一直阻塞
     */
    void lock();

    /**
     * 获取分布式锁, 不管抢锁成功与否, 都会立即返回
     *
     * @return true-抢锁成功, false-抢锁失败
     */
    boolean tryLock();

    /**
     * 获取分布式锁, 抢到锁时立即返回; 未抢到锁等待指定时长后, 仍不能抢锁则返回
     *
     * @param time     等待时长
     * @param timeUnit 等待时间单位
     *
     * @return true-抢锁成功, false-抢锁失败
     */
    boolean tryLock(long time, TimeUnit timeUnit);

    /**
     * 解锁
     */
    void unlock();
}
