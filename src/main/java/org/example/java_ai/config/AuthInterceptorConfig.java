package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置
 * 认证逻辑已由 Spring Security + JwtAuthenticationFilter 接管
 */
@Slf4j
@Configuration
public class AuthInterceptorConfig implements WebMvcConfigurer {
}
