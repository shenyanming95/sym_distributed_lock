package com.sym.config;

import com.sym.bus.MessageBus;
import com.sym.constant.LockConstants;
import com.sym.enums.MessageTypeEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * 分布式锁的配置类
 *
 * @author shenyanming
 * Created on 2020/8/14 09:27
 */
@Configuration
public class LockConfig {

    /**
     * 启动时需要运行的方法
     */
    @PostConstruct
    public void initWhenStart() {
        startMessageBus();
    }

    /**
     * 启动消息总线
     */
    private void startMessageBus() {
        MessageBus.start();
    }

    /**
     * redis配置类
     */
    @Configuration
    @ConditionalOnProperty(prefix = "lock.strategy", havingValue = "redis")
    static class RedisConfig {

        /**
         * 配置 redis 监听容器
         */
        @Bean
        public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
                                                                           MessageListenerAdapter messageListenerAdapter) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redisConnectionFactory);
            // 表示监听 _$redis_$lock 这个通道, 用 messageListenerAdapter 这个适配器去处理通道的消息
            container.addMessageListener(messageListenerAdapter, new PatternTopic(LockConstants.REDIS_LOCK_CHANNEL));
            /*
             * 这里还可以添加多个监听通道, 全部加到监听容器里 RedisMessageListenerContainer
             * container.addMessageListener(messageListenerAdapter,new PatternTopic("__keyevent@0__:expired"));
             * container.addMessageListener(messageListenerAdapter,new PatternTopic("__keyevent@0__:del"));
             */
            return container;
        }

        /**
         * redis消息处理器
         */
        @Bean
        public MessageListenerAdapter messageListenerAdapter() {
            // 指定处理通道消息的类和方法
            return new MessageListenerAdapter(new RedisKeyResolver(), "handleMessage");
        }

    }

    /**
     * zookeeper配置类
     */
    @Configuration
    @ConditionalOnProperty(prefix = "lock.strategy", havingValue = "zookeeper")
    static class ZookeeperConfig {

    }

    /**
     * redis 发布-订阅 模式的消息处理器
     */
    private static class RedisKeyResolver {
        /**
         * 处理redis通道上的消息
         *
         * @param key key
         */
        public void handleMessage(String key) {
            if (!StringUtils.isEmpty(key)) {
                MessageBus.Message message = new MessageBus.Message(MessageTypeEnum.RELEASE_LOCK, key);
                MessageBus.publish(message);
            }
        }
    }
}
