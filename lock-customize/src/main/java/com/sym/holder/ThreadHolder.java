package com.sym.holder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局单例, 维护了key+thread的关联关系
 *
 * @author shenyanming
 * Created on 2020/6/23 17:11
 */
public class ThreadHolder {

    public final static ThreadHolder INSTANCE;
    private final static Map<String, List<Thread>> THREAD_MAP;

    static {
        INSTANCE = new ThreadHolder();
        THREAD_MAP = new ConcurrentHashMap<>();
    }

    private ThreadHolder() {
    }

    /**
     * 关联分布式锁key和线程thread
     * @param lockKey 分布式锁key
     * @param thread 线程
     */
    public static void put(String lockKey, Thread thread){
        put(lockKey, Collections.singleton(thread));
    }

    /**
     * 将分布式锁key和线程关联起来
     * @param lockKey 分布式锁key
     * @param threads 线程集
     */
    public static void put(String lockKey, Collection<Thread> threads) {
        List<Thread> threadList = THREAD_MAP.computeIfAbsent(lockKey, (key) -> new CopyOnWriteArrayList<>());
        threadList.addAll(threads);
    }

    /**
     * 通过分布式锁key获取线程集合
     * @param lockKey 分布式锁key
     * @return 线程集
     */
    public static List<Thread> get(String lockKey){
        List<Thread> threadList = THREAD_MAP.get(lockKey);
        return Objects.isNull(threadList) ? Collections.emptyList() : threadList;
    }

    /**
     * 获取维护的所有分布式锁key
     * @return 键集合
     */
    public static Set<String> getKeyList(){
        return THREAD_MAP.keySet();
    }
}
