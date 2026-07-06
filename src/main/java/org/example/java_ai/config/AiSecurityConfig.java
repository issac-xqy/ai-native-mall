package org.example.java_ai.config;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI接口安全服务 - API Key 鉴权、限流、敏感词过滤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSecurityConfig {

    private final RedisRateLimiter redisRateLimiter;

    private final Set<String> validApiKeys = ConcurrentHashMap.newKeySet();

    private final SensitiveWordBs sensitiveWordBs = SensitiveWordBs.newInstance()
            .enableWordCheck(true)
            .init();

    /**
     * 注册有效 API Key（生产环境应从数据库或配置中心同步）
     */
    public void registerApiKey(String apiKey) {
        if (apiKey != null && apiKey.startsWith("sk-")) {
            validApiKeys.add(apiKey);
        }
    }

    public boolean checkRateLimit(String userId, String apiKey) {
        boolean qpsOk = redisRateLimiter.tryAcquire("qps:" + userId, 10, 1);
        if (!qpsOk) {
            log.warn("用户 {} 触发QPS限流", userId);
            return false;
        }
        boolean dailyOk = redisRateLimiter.tryAcquire("daily:" + apiKey, 10000, 86400);
        if (!dailyOk) {
            log.warn("API Key {} 达到日调用上限", apiKey);
            return false;
        }
        return true;
    }

    public String filterSensitiveWords(String text) {
        if (text == null || text.isEmpty()) return text;
        return sensitiveWordBs.replace(text);
    }

    /**
     * 是否启用了 API Key 校验（有注册的白名单 key 时才启用）
     */
    public boolean isApiKeyCheckEnabled() {
        return !validApiKeys.isEmpty();
    }

    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return false;
        if (!apiKey.startsWith("sk-")) return false;
        return validApiKeys.contains(apiKey);
    }

    public void revokeApiKey(String apiKey) {
        validApiKeys.remove(apiKey);
        log.info("API Key 已撤销: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
    }
}
