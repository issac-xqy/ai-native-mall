package org.example.java_ai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 全上下文启动测试 — 需要 MySQL 和 Redis 连接
 * 本地开发跳过，CI 环境启用
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("需要 MySQL + Redis 容器，CI 环境运行")
class JavaAiApplicationTests {

    @Test
    void contextLoads() {
    }

}
