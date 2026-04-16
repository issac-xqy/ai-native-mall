package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executors;

/**
 * JDK 17 线程池配置 - 兼容模式
 * 
 * 由于 JDK 17 不支持虚拟线程，我们使用传统的 ThreadPoolTaskExecutor。
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * 配置传统线程执行器
     * 用于AI接口调用、商品描述生成等耗时操作
     */
    @Bean("virtualTaskExecutor")
    public TaskExecutor virtualTaskExecutor() {
        log.info("初始化JDK 17传统线程执行器");
        
        var executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ai-task-");
        executor.initialize();
        return executor;
    }

    /**
     * 配置AI专用线程池
     * 用于流式响应、RAG检索等场景
     */
    @Bean("aiTaskExecutor")
    public TaskExecutor aiTaskExecutor() {
        log.info("初始化AI专用线程池");
        
        var executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ai-stream-");
        executor.initialize();
        return executor;
    }
}
