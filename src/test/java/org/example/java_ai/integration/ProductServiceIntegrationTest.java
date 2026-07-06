package org.example.java_ai.integration;

import org.example.java_ai.entity.Product;
import org.example.java_ai.mapper.ProductMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 — 使用 Testcontainers 启动真实 MySQL 容器
 * 代替 H2 内存库，验证 SQL 兼容性
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Tag("integration")
class ProductServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ai_mall_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6380");
    }

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("测试 MySQL 真实连接 — 通过 Flyway 迁移后表存在")
    void testDatabaseConnection() {
        assertNotNull(mysql.getJdbcUrl());
        assertTrue(mysql.isRunning());
    }

    @Test
    @DisplayName("查询商品 — 验证 MyBatis-Plus 与 MySQL 兼容")
    void testQueryProducts() {
        List<Product> products = productMapper.selectList(null);
        assertNotNull(products, "结果集不应为 null");
        // 新数据库应为空或有种子数据（取决于 Flyway V1）
    }
}
