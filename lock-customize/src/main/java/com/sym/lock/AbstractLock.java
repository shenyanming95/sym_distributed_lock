package com.sym.lock;

import com.sym.holder.ThreadHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 抽象父类, 实现了加锁、解锁、定时任务的功能
 *
 * @author shenyanming
 * Created on 2020/8/13 18:09
 */
@Slf4j
public abstract class AbstractLock implements ILock {

    //--------------------------------------------------------------- static field

    /**
     * 全局唯一的 key-thread 持有
     */
    private static ThreadHolder threadHolder = ThreadHolder.INSTANCE;

    /**
     * 定时调度
     */
    private static ScheduledExecutorService scheduledExecutor;

    /**
     * 标识是否启动了定时任务, 仅仅会在真正开始使用时才启动定时任务
     */
    private static AtomicBoolean isStarted = new AtomicBoolean();


    //--------------------------------------------------------------- field

    /**
     * 分布式锁的key
     */
    protected String key;

    /**
     * 线程标识
     */
    protected String threadId;

    protected AbstractLock(String key, String threadId){
        this.key = key;
        this.threadId = threadId;
    }

    static {
        scheduledExecutor = Executors.newScheduledThreadPool(3);
        // 钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduledExecutor.shutdown()));
    }

    @Override
    public void lock() throws InterruptedException {
        Thread t = Thread.currentThread();
        for (; ; ) {
            // get lock, then return
            if (tryRequire()) return;
            // lose lock, then interrupt
            if (threadHolder.put(key, t)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    throw new InterruptedException("thread interrupt");
                }
            }
        }
    }

    @Override
    public boolean tryLock() {
        return tryRequire();
    }

    @Override
    public boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException {
        long needTime = timeUnit.toNanos(time);
        long deadTime = needTime + System.nanoTime();
        Thread t = Thread.currentThread();
        for (; ; ) {
            if (tryRequire()) return true;
            needTime = deadTime - System.nanoTime();
            if (needTime <= 0L) {
                // 等待时间点到, 还未获取到锁, 返回
                return false;
            }
            // 返回时间点未到, 挂起线程
            if (threadHolder.put(key, t)) {
                LockSupport.parkNanos(this, needTime);
                if (Thread.interrupted()) {
                    throw new InterruptedException("thread interrupt");
                }
            }
        }
    }

    @Override
    public void unlock() {
        if (tryRelease()) {
            // 释放锁成功, 唤醒本地阻塞的线程.
            // 分布式集群的其它节点, 在 tryRelease() 具体实现中去释放信号
            Iterator<Thread> iterator = threadHolder.get(key).iterator();
            while (iterator.hasNext()) {
                Thread thread = iterator.next();
                LockSupport.unpark(thread);
                iterator.remove();
            }
        }
    }

    /**
     * 实际资源申请、释放
     */
    protected abstract boolean tryRequire();

    protected abstract boolean tryRelease();

}
