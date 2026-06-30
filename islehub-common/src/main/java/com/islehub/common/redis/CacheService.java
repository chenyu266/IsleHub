package com.islehub.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 通用缓存服务：Cache-Aside 模式
 * 三重防护：
 * 1. 防穿透：缓存空值标记 ##NULL##，TTL 2 分钟
 * 2. 防击穿：SETNX 互斥锁，sleep 50ms 重试，最多 10 次 + 双重检查
 * 3. 防雪崩：TTL 加 0~10% 随机抖动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private static final String NULL_MARKER = "##NULL##";
    private static final String LOCK_SUFFIX = ":lock";
    private static final long LOCK_WAIT_MS = 50L;
    private static final int MAX_RETRY = 10;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 先读缓存，未命中则执行 loader 回源并写入缓存
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        // 1. 先查缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (NULL_MARKER.equals(cached)) {
            return null; // 防穿透：空值标记
        }
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, type);
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存反序列化失败, key={}, 将回源", key, e);
            }
        }

        // 2. 缓存未命中，尝试获取互斥锁（防击穿）
        String lockKey = key + LOCK_SUFFIX;
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5)));

        if (!locked) {
            // 未获取锁，短暂休眠后重试读缓存
            for (int i = 0; i < MAX_RETRY; i++) {
                try {
                    Thread.sleep(LOCK_WAIT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                cached = redisTemplate.opsForValue().get(key);
                if (NULL_MARKER.equals(cached)) {
                    return null;
                }
                if (cached != null) {
                    try {
                        return objectMapper.readValue(cached, type);
                    } catch (JsonProcessingException e) {
                        log.warn("Redis 重试反序列化失败, key={}", key);
                    }
                }
                locked = Boolean.TRUE.equals(
                        redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5)));
                if (locked) break;
            }

            // 重试耗尽仍未获取锁，直接回源（避免死等）
            if (!locked) {
                log.warn("缓存未获取锁且重试耗尽，直接回源, key={}", key);
                return loader.get();
            }
        }

        // 3. 获取到锁，双重检查（防止其他线程已写入）
        try {
            cached = redisTemplate.opsForValue().get(key);
            if (NULL_MARKER.equals(cached)) {
                return null;
            }
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, type);
                } catch (JsonProcessingException e) {
                    log.warn("Redis 双重检查反序列化失败, key={}", key);
                }
            }

            // 4. 回源加载
            T result = loader.get();
            if (result == null) {
                // 防穿透：缓存空值，TTL 2 分钟
                redisTemplate.opsForValue().set(key, NULL_MARKER, Duration.ofMinutes(2));
            } else {
                // 防雪崩：TTL 加 0~10% 随机抖动
                long jitterMillis = (long) (ttl.toMillis() * ThreadLocalRandom.current().nextDouble(0.0, 0.1));
                Duration finalTtl = ttl.plusMillis(jitterMillis);
                try {
                    String json = objectMapper.writeValueAsString(result);
                    redisTemplate.opsForValue().set(key, json, finalTtl);
                } catch (JsonProcessingException e) {
                    log.error("Redis 缓存序列化失败, key={}", key, e);
                }
            }
            return result;
        } finally {
            // 5. 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 批量失效缓存
     */
    public void evict(String... keys) {
        if (keys != null && keys.length > 0) {
            redisTemplate.delete(Arrays.asList(keys));
        }
    }
}
