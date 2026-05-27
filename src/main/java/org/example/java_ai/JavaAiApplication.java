package org.example.java_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI-Native智能商城主启动类
 * 
 * 技术栈：
 * - Spring Boot 3.2 + JDK 21 (虚拟线程)
 * - Spring AI + LangChain4j (AI能力)
 * - Spring Cloud Alibaba (微服务治理)
 * - Redis向量库 + Elasticsearch (混合检索)
 * 
 * @author xqy
 * @since 2026-04-09
 */
@SpringBootApplication
@EnableDiscoveryClient // 启用Nacos服务发现
@EnableAsync // 启用异步支持（虚拟线程）
public class JavaAiApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "false");
        
        SpringApplication.run(JavaAiApplication.class, args);
    }

}
