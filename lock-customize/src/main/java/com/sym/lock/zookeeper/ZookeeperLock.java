package com.sym.lock.zookeeper;

import com.sym.lock.AbstractLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.core.env.Environment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.sym.constant.LockConstants.ZOOKEEPER_HOST_PROPERTIES;

/**
 * 自己实现的分布式锁, 利用zookeeper创建唯一路径的ZNode节点的特性.
 *
 * @author shenyanming
 * Created on 2020/8/14 10:48
 */
@Slf4j
public class ZookeeperLock extends AbstractLock {

    private static CuratorFramework zkClient;
    private static Environment env;
    private static String LOCK_PREFIX = "/_lock_/";
    private static Charset charset = StandardCharsets.UTF_8;

    static {
        try {
            init();
        } catch (Exception e) {
            log.error("初始化Zookeeper失败, ", e);
            System.exit(1);
        }
    }

    /**
     * 初始化Zookeeper客户端, 原生的zk客户端不好用, 使用Curator替代
     */
    private static void init() {
        // 获取zookeeper地址
        String zkHost = env.getProperty(ZOOKEEPER_HOST_PROPERTIES, "localhost:2181");
        // zk客户端与zk服务端的会话超时, 单位毫秒
        int sessionTimeOut = 60 * 1000;
        // zk客户端与zk服务端的连接超时, 单位毫秒
        int connectionTimeOut = 30 * 1000;
        // 重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        // 获取建造器
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        builder.connectString(zkHost)
                .sessionTimeoutMs(sessionTimeOut)
                .connectionTimeoutMs(connectionTimeOut)
                .retryPolicy(retryPolicy);
        CuratorFramework framework = builder.build();
        framework.start();
        zkClient = framework;
    }

    public ZookeeperLock(String key) {
        super(LOCK_PREFIX + key, UUID.randomUUID().toString());
    }

    @Override
    protected boolean tryRequire() {
        try {
            String result = zkClient.create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(key, threadId.getBytes(charset));
            log.info("获取分布式锁, key:{}, info:{}", key, result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean tryRelease() {
        try {
            byte[] data = zkClient.getData().forPath(key);
            if (data != null && threadId.equals(new String(data, charset))) {
                zkClient.delete().forPath(key);
                return true;
            }
        } catch (Exception e) {
            log.warn("异常, ", e);
        }
        return false;
    }


}
