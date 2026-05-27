package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * 静态资源映射配置
 * 用于访问上传的文件
 */
@Slf4j
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录
        String projectPath = System.getProperty("user.dir");
        String uploadPath = Paths.get(projectPath, "uploads").toFile().getAbsolutePath();
        
        log.info("静态资源映射: {} -> file:{}", "/api/file/**", uploadPath);
        
        // 映射上传文件目录
        registry.addResourceHandler("/api/file/**")
                .addResourceLocations("file:" + uploadPath + File.separator)
                .setCachePeriod(3600)  // 缓存1小时
                .resourceChain(true);
    }
}
