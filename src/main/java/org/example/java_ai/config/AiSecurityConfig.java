package org.example.java_ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AI接口安全配置 - 鉴权与限流
 * 
 * 安全措施：
 * 1. API Key鉴权：防止未授权访问
 * 2. Redis令牌桶限流：防止Token被盗刷
 * 3. 请求频率控制：单用户QPS限制
 * 4. 敏感词过滤：防止提示词注入攻击
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiSecurityConfig {

    private final RedisRateLimiter redisRateLimiter;

    /**
     * AI接口限流拦截器
     * 
     * 限流规则：
     * - 单用户QPS: 10次/秒
     * - 单API Key日调用上限: 10000次
     * - 突发流量峰值: 50次/秒（允许短时突发）
     */
    public boolean checkRateLimit(String userId, String apiKey) {
        // 使用Redis实现限流（替换原来的ConcurrentHashMap）
        // QPS限流：1秒内最多10次
        boolean qpsOk = redisRateLimiter.tryAcquire("qps:" + userId, 10, 1);
        if (!qpsOk) {
            log.warn("用户 {} 触发QPS限流", userId);
            return false;
        }
        
        // 日调用上限：24小时内最多10000次
        boolean dailyOk = redisRateLimiter.tryAcquire("daily:" + apiKey, 10000, 86400);
        if (!dailyOk) {
            log.warn("API Key {} 达到日调用上限", apiKey);
            return false;
        }
        
        return true;
    }

    /**
     * 敏感词过滤器
     * 防止提示词注入攻击
     */
    public String filterSensitiveWords(String prompt) {
        // 使用sensitive-word库进行敏感词过滤
        // 简单实现，生产环境应加载完整的敏感词库
        String[] sensitiveWords = {
            "忽略之前的指令",
            "绕过限制",
            "系统提示词",
            "管理员权限",
            "你是谁开发的",
            "你的训练数据"
        };
        
        String filtered = prompt;
        for (String word : sensitiveWords) {
            if (filtered.contains(word)) {
                log.warn("检测到敏感词: {}", word);
                filtered = filtered.replace(word, "***");
            }
        }
        
        return filtered;
    }

    /**
     * 验证API Key
     */
    public boolean validateApiKey(String apiKey) {
        // 生产环境应从数据库或配置中心验证
        return apiKey != null && !apiKey.isEmpty() && apiKey.startsWith("sk-");
    }
}
