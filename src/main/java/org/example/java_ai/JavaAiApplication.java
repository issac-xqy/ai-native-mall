package org.example.java_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI-Native智能商城主启动类
 *
 * 技术栈：
 * - Spring Boot 3.2 + JDK 21
 * - LangChain4j (AI 能力)
 * - MyBatis Plus (ORM)
 * - Redis (缓存 + 向量检索)
 * - MySQL (业务数据)
 *
 * @author xqy
 * @since 2026-04-09
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
public class JavaAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaAiApplication.class, args);
    }

}
