package org.example.java_ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis限流器 - 基于令牌桶算法
 * 替换原来的ConcurrentHashMap内存实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查是否允许请求
     * 
     * @param key 限流键（用户ID或API Key）
     * @param limit 限制次数
     * @param window 时间窗口（秒）
     * @return true-允许 false-拒绝
     */
    public boolean tryAcquire(String key, int limit, long window) {
        String redisKey = "rate_limit:" + key;
        
        try {
            // 获取当前计数
            Long count = redisTemplate.opsForValue().increment(redisKey);
            
            // 如果是第一次访问，设置过期时间
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
            }
            
            // 检查是否超过限制
            if (count != null && count <= limit) {
                return true;
            }
            
            log.warn("用户 {} 触发限流，当前计数: {}", key, count);
            return false;
        } catch (Exception e) {
            log.error("Redis限流检查失败，降级为拒绝请求", e);
            return false;
        }
    }

    /**
     * 获取 key 的 TTL（秒）
     */
    public long getTtl(String key) {
        String redisKey = "rate_limit:" + key;
        try {
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        String redisKey = "rate_limit:" + key;
        redisTemplate.delete(redisKey);
    }
}
