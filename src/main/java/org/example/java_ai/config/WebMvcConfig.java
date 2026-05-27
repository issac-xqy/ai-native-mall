package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Web MVC 配置
 * 解决 Spring MVC 异步请求默认使用 SimpleAsyncTaskExecutor 的警告
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcTaskExecutor());
        configurer.setDefaultTimeout(60000); // 异步请求超时时间 60s
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径（商品图片、头像等）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
        log.info("✅ 配置静态资源访问路径: /uploads/** -> file:./uploads/");
    }

    @Bean("mvcTaskExecutor")
    public AsyncTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("初始化 MVC 异步线程池 (Core: 20, Max: 100)");
        return executor;
    }
}
