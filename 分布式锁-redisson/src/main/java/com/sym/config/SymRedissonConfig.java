package com.sym.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * redisson配置类, 用来注入一个{@link RedissonClient}
 *
 * @author shenym
 * @date 2019/11/23 13:36
 */
@Configuration
public class SymRedissonConfig {

    @Bean
    public Config redissonConfig() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("singleConfig.yaml");
        return Config.fromYAML(inputStream);
    }

    @Bean
    public RedissonClient redissonClient(Config redissonConfig) {
        return Redisson.create(redissonConfig);
    }

}
