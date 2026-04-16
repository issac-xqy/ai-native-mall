package org.example.java_ai.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 微服务配置
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Configuration
public class MicroserviceConfig {

    /**
     * 配置负载均衡的RestTemplate
     * 用于服务间调用
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
