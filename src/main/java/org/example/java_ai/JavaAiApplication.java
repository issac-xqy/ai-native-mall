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
        // JDK 17 兼容模式
        System.setProperty("spring.threads.virtual.enabled", "false");
        
        SpringApplication.run(JavaAiApplication.class, args);
        
        System.out.println("""
                
                ========================================
                  AI-Native Smart Mall 启动成功！
                  
                  技术栈:
                  - Spring Boot 3.2 + JDK 17 (兼容模式)
                  - Spring AI + LangChain4j
                  - Nacos + Sentinel + Gateway
                  - Redis Vector + Elasticsearch
                  
                  API文档:
                  - 智能客服: POST /api/ai/customer-service/ask
                  - 流式对话: GET /api/ai/customer-service/stream
                  - SEO标题: POST /api/ai/product/seo-title
                  - 商品描述: POST /api/ai/product/description
                  - 评论分析: POST /api/ai/comment/analyze
                  ========================================
                """);
    }

}
