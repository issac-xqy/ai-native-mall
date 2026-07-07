package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * 异步任务线程池配置 — JDK 21 虚拟线程模式
 *
 * 注：LangChain4j 同步 HTTP 调用在虚拟线程中可能触发 carrier thread pinning。
 * 如果生产环境出现平台线程耗尽，将此 Bean 回退为 ThreadPoolTaskExecutor。
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean("virtualTaskExecutor")
    public Executor virtualTaskExecutor() {
        var executor = new SimpleAsyncTaskExecutor("async-ai-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(200);
        log.info("虚拟线程 Executor 已初始化: async-ai- (concurrencyLimit=200)");
        return executor;
    }

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        var executor = new SimpleAsyncTaskExecutor("ai-stream-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(100);
        log.info("虚拟线程 Executor 已初始化: ai-stream- (concurrencyLimit=100)");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> {
            log.error("Async 方法 [{}] 未捕获异常, 参数个数={}", method.getName(), params.length, ex);
        };
    }
}
