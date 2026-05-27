package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * CORS 配置已由 SecurityConfig#corsConfigurationSource 统一管理
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(name = "corsConfigurationSource")
public class CorsConfig {
    static {
        log.info("注意: CORS 由 SecurityConfig 统一管理");
    }
}
