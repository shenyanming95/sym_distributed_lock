package com.sym.lock;

import com.sym.holder.ThreadHolder;

import java.util.concurrent.TimeUnit;

/**
 * 抽象父类, 实现了加锁、解锁、定时任务的功能
 *
 * @author shenyanming
 * Created on 2020/8/13 18:09
 */
public abstract class AbstractLock implements ILock {

    //--------------------------------------------------------------- static field

    /**
     * 全局唯一的 key-thread 持有
     */
    private static ThreadHolder threadHolder = ThreadHolder.INSTANCE;


    //--------------------------------------------------------------- field

    /**
     * 分布式锁的key
     */
    private String key;



    @Override
    public void lock() {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit timeUnit) {
        return false;
    }

    @Override
    public void unlock() {

    }


    /**
     * 实际资源申请、释放
     */
    protected abstract boolean tryRequire();
    protected abstract boolean tryRelease();

}
