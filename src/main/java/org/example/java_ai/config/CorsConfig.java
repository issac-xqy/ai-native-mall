package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许跨域的源
        config.addAllowedOriginPattern("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许的 HTTP 方法
        config.addAllowedMethod("*");
        
        // 允许携带 Cookie
        config.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        
        // 暴露的响应头
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Authorization");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        log.info("✅ CORS 跨域配置已启用");
        
        return new CorsFilter(source);
    }
}
