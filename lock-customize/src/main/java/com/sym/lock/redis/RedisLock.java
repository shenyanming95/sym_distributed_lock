package com.sym.lock.redis;

import com.sym.constant.LockConstants;
import com.sym.lock.AbstractLock;
import com.sym.util.SpringContextUtil;
import io.lettuce.core.RedisNoScriptException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 自己实现的分布式锁,利用redis执行lua脚本是原子性的特点,使用lua脚本来完成加锁逻辑与解锁逻辑.
 * <p>
 * 加锁脚本解释:
 * 1)、使用exists判断key是否存在, 如果不存在, 执行 hmset+expire 命令, 表示获取到锁,lua脚本返回1(表示加锁成功)
 * 2)、如果key已经存在, 使用 hget 命令判断 hash 值里面的 threadId 是否是当前申请加锁的线程唯一标识, 如果不是说明此时锁被另一个线程占用, lua脚本返回0(表示加锁失败)
 * 3)、如果key已经存在, 并且hash值里面的threadId是当前申请加锁的线程, 则将hash值里面的count累加1, 表示锁重入次数+1, 重新设置过期时间, lua脚本返回1(表示加锁成功)
 * </p>
 * <p>
 * 解锁脚本解释：
 * 1)、使用exists判断key是否存在, 如果不存在, 说明没有线程在占用锁, 也就没必要解锁, lua脚本返回0(表示解锁失败)
 * 2)、如果key已经存在, 使用 hget 命令判断hash值里面的threadId是否是当前申请解锁的线程, 如果不是说明当前锁不是被该线程占用, 它就没资格解锁, lua脚本返回0(表示解锁失败)
 * 3)、如果hash值内的threadId是当前申请解锁的线程, 将hash值内的count减1, 若count不等于0, 说明解锁次数仍小于加锁次数, lua脚本返回0(表示解锁失败)
 * 4)、若count值减1后等于0, 说明解锁次数==加锁次数, 删除key, 并使用publish命令发布一条删除key的消息
 * </p>
 *
 * @author shenyanming
 * Created on 2020/8/14 10:05
 */
@Slf4j
public class RedisLock extends AbstractLock {

    static {
        // 获取redisTemplate
        redisTemplate = SpringContextUtil.getBean(StringRedisTemplate.class);

        // 构建脚本
        buildScript();

        // 缓存脚本
        scriptCache();
    }

    /**
     * 加锁lua脚本、解锁lua脚本
     */
    private static String LOCK_SCRIPT;
    private static String UNLOCK_SCRIPT;

    /**
     * 加锁lua脚本缓存、解锁lua脚本缓存
     */
    private static String LOCK_SCRIPT_SHA;
    private static String UNLOCK_SCRIPT_SHA;

    /**
     * 标识脚本缓存初始化
     */
    private static volatile boolean isInit = false;

    /**
     * redis操作
     */
    private static StringRedisTemplate redisTemplate;

    private static Charset charset = StandardCharsets.UTF_8;
    private static int lockTime = 50;

    /**
     * 表示此实例的线程ID
     */
    private String threadId;


    public RedisLock(String key) {
        super(key, UUID.randomUUID().toString().replace("-", ""));
    }


    @Override
    protected boolean tryRequire() {
        boolean result;
        try {
            result = doLock(lockTime);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RedisNoScriptException) {
                //由于redis的脚本缓存被清空了,重新缓存脚本
                scriptCache();
                //重新申请锁
                result = doLock(lockTime);
            } else {
                log.error("加锁异常，原因：{}", e.getMessage());
                throw e;
            }
        }
        return result;
    }

    @Override
    protected boolean tryRelease() {
        boolean result = false;
        try {
            result = this.doUnlock();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RedisNoScriptException) {
                //由于redis的脚本缓存被清空了,重新缓存脚本
                scriptCache();
                //重新申请锁
                result = this.doUnlock();
            } else {
                log.error("解锁异常，原因：{}", e.getMessage());
                throw e;
            }
        }
        return result;
    }

    /**
     * 实际加锁逻辑
     *
     * @param ttlTime 加锁时间
     * @return true-获得锁,false-未获取到锁
     */
    private boolean doLock(int ttlTime) {
        Boolean result = evalSha(LOCK_SCRIPT_SHA, key, "uuid", "count", threadId, "1", Integer.toString(ttlTime));
        boolean f = result == null ? false : result;
        if (f) {
            log.info("线程[{}]获取到锁[{}]", Thread.currentThread().getName(), key);
        }
        return f;
    }

    /**
     * 实际解锁逻辑
     *
     * @return true-解锁成功,false-解锁失败
     */
    private boolean doUnlock() {
        // 执行脚本
        Boolean result = evalSha(UNLOCK_SCRIPT_SHA, key, "uuid", "count", threadId);
        boolean f = result == null ? false : result;
        if (f) {
            log.info("线程[{}]已成功解锁[{}]", Thread.currentThread().getName(), key);
        }
        return f;
    }

    /**
     * 执行redis脚本缓存
     *
     * @param scriptSha   脚本缓存
     * @param keysAndArgs 参数
     * @return 执行结果
     */
    private Boolean evalSha(String scriptSha, String... keysAndArgs) {
        byte[][] params = new byte[keysAndArgs.length][];
        for (int i = 0, len = keysAndArgs.length; i < len; i++) {
            params[i] = keysAndArgs[i].getBytes(charset);
        }
        return redisTemplate.execute((RedisCallback<Boolean>) conn -> {
            Long o = conn.evalSha(scriptSha, ReturnType.INTEGER, 3, params);
            return o != null && o == 1L;
        });
    }

    /**
     * 构建加锁、解锁脚本
     */
    private static void buildScript() {
        // 加锁脚本:
        // 外层键 -- KEYS[1]
        // 线程ID -- KEYS[2]  ARGV[1]
        // 加锁次数 -- KEYS[3]  ARGV[2]
        // 超时时间(秒) -- ARGV[3]
        StringBuilder sb = new StringBuilder();
        sb.append("if(redis.call('exists',KEYS[1]) == 1) then ")
                .append("if(redis.call('hget',KEYS[1],KEYS[2]) == ARGV[1]) then ")
                .append("redis.call('hincrby',KEYS[1],KEYS[3],1) ")
                .append("redis.call('expire',KEYS[1],ARGV[3]) ")
                .append("return 1 else return 0 end ")
                .append("redis.call('hmset',KEYS[1],KEYS[2],ARGV[1],KEYS[3],ARGV[2])")
                .append("redis.call('expire',KEYS[1],ARGV[3])")
                .append("return 1 end");
        LOCK_SCRIPT = sb.toString();

        // 解锁脚本:
        // 外层键 -- KEYS[1]
        // 线程ID -- KEYS[2]  ARGV[1]
        // 加锁次数 -- KEYS[3]
        sb = new StringBuilder();
        sb.append("if(redis.call('exists',KEYS[1]) == 1) then ")
                .append("if(redis.call('hget',KEYS[1],KEYS[2]) == ARGV[1]) then ")
                .append("local count = redis.call('hincrby',KEYS[1],KEYS[3],-1) ")
                .append("if(count==0) then redis.call('del',KEYS[1]) ")
                .append("redis.call('publish','").append(LockConstants.REDIS_LOCK_CHANNEL).append("',KEYS[1]) return 1 ")
                .append("else return 0 end ")
                .append("else return 0 end ")
                .append("else return 0 end");
        UNLOCK_SCRIPT = sb.toString();
    }

    /**
     * 缓存加锁、解锁脚本
     */
    private static void scriptCache() {
        LOCK_SCRIPT_SHA = redisTemplate.execute((RedisCallback<String>) conn -> conn.scriptLoad(LOCK_SCRIPT.getBytes(charset)));
        UNLOCK_SCRIPT_SHA = redisTemplate.execute((RedisCallback<String>) conn -> conn.scriptLoad(UNLOCK_SCRIPT.getBytes(charset)));
    }
}
