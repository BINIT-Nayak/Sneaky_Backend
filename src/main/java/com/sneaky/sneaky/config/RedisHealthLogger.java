package com.sneaky.sneaky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthLogger {
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void logRedisHealth() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            String ping = connection.ping();
            log.info(
                    "Redis connection verified on startup. host={}, port={}, sslEnabled={}, ping={}",
                    redisHost,
                    redisPort,
                    redisSslEnabled,
                    ping);
        } catch (RuntimeException ex) {
            log.warn(
                    "Redis connection check failed on startup. host={}, port={}, sslEnabled={}",
                    redisHost,
                    redisPort,
                    redisSslEnabled,
                    ex);
        }
    }
}
