package com.sym.constant;

/**
 * @author shenyanming
 * Created on 2020/8/13 17:19
 */
public class LockConstants {

    /**
     * 使用Redis当做分布式锁, 当释放锁时, 会发布消息到此通道(自定义)
     */
    public final static String REDIS_LOCK_CHANNEL = " _$redis_$lock";

    /**
     * zookeeper地址
     */
    public final static String ZOOKEEPER_HOST_PROPERTIES = "zookeeper.host";
}
