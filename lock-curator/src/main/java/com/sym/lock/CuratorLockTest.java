package com.sym.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMultiLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * @author shenyanming
 * Created on 2020/8/14 08:56
 */
public class CuratorLockTest {

    private CuratorFramework curator;

    @Before
    public void init(){
        // zk客户端与zk服务端的会话超时, 单位毫秒
        int sessionTimeOut = 60 * 1000;
        // zk客户端与zk服务端的连接超时, 单位毫秒
        int connectionTimeOut = 30 * 1000;
        // 重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // 获取建造器
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        builder.connectString("127.0.0.1:2181")
                .sessionTimeoutMs(sessionTimeOut)
                .connectionTimeoutMs(connectionTimeOut)
                .retryPolicy(retryPolicy);
        CuratorFramework framework = builder.build();
        framework.start();
        curator = framework;
    }

    /**
     * 可重入共享锁
     */
    @Test
    public void interProcessMutex() {
        String path = "/test";
        InterProcessMutex interProcessMutex = new InterProcessMutex(curator, path);
    }

    /**
     * 不可重入共享锁
     */
    @Test
    public void interProcessSemaphoreMutex() {
        String path = "/test";
        InterProcessSemaphoreMutex interProcessSemaphoreMutex = new InterProcessSemaphoreMutex(curator, path);
    }

    /**
     * 可重入读写锁
     */
    @Test
    public void interProcessReadWriteLock() {
        String path = "/test";
        InterProcessReadWriteLock interProcessReadWriteLock = new InterProcessReadWriteLock(curator, path);
    }

    /**
     * 信号量
     */
    public void InterProcessSemaphoreV2() {
        String path = "/test";
        InterProcessSemaphoreV2 interProcessSemaphoreV2 = new InterProcessSemaphoreV2(curator, path, 2);
    }

    /**
     * 组锁实现, 一个锁的获取和释放, 都会传递相同操作给它包含的其它锁
     */
    @Test
    public void interProcessMultiLock() {
        String path = "/test";
        // 子锁
        InterProcessLock lock1 = new InterProcessMutex(curator, path);
        InterProcessLock lock2 = new InterProcessSemaphoreMutex(curator, path);

        // 组锁
        InterProcessMultiLock lock = new InterProcessMultiLock(Arrays.asList(lock1, lock2));
    }
}
