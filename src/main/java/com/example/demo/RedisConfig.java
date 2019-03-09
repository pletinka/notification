
package com.example.demo;

import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class RedisConfig {
    @Value(value = "${redis.host}")
    private String redisHost;

    @Bean
    public RedisClient redisClient() {
        RedisClient redisClient = RedisClient.create(redisHost);
        redisClient.setDefaultTimeout(Duration.of(24, ChronoUnit.HOURS));
        return redisClient;
    }
}
