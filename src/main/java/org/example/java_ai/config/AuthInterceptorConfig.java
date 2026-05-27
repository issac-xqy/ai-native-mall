package org.example.java_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.util.TokenUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 登录拦截器配置
 */
@Slf4j
@Configuration
public class AuthInterceptorConfig implements WebMvcConfigurer {

    // 不需要拦截的白名单路径
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/user/login",
        "/api/user/register",
        "/api/product/list",
        "/api/product/**",  // 匹配商品相关所有接口
        "/api/statistics/**", // 匹配统计相关所有接口
        "/api/admin/**",    // 管理后台接口（暂时放行，后续可加独立鉴权）
        "/api/order/*/ship", // 管理后台发货接口
        "/api/upload/**",   // 文件上传接口
        "/uploads/**",       // 静态资源访问
        "/api/ai/customer-service/**", // AI智能客服（已有apiKey鉴权）
        "/api/ai/**", // AI能力接口（暂时放行，后续可加独立鉴权）
        "/api/search/**"     // 语义搜索接口（公开查询）
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")  // 拦截所有/api/**路径
                .excludePathPatterns(WHITE_LIST);  // 排除白名单
    }

    /**
     * 登录拦截器
     */
    private static class AuthInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String uri = request.getRequestURI();
            String method = request.getMethod();

            log.info("请求拦截 - URI: {}, Method: {}", uri, method);

            // 放行OPTIONS请求（CORS预检）
            if ("OPTIONS".equals(method)) {
                return true;
            }

            // 获取Token（从Header或Parameter）
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                token = request.getParameter("token");
            }

            // 检查Token是否存在
            if (token == null || token.isEmpty()) {
                log.warn("未提供Token，拒绝访问: {}", uri);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"请先登录\",\"code\":401}");
                return false;
            }

            // 验证Token有效性并解析用户ID
            if (!TokenUtil.isTokenValid(token)) {
                log.warn("Token无效或已过期，拒绝访问: {}", uri);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"登录已过期，请重新登录\",\"code\":401}");
                return false;
            }

            // 解析用户ID并存储到request中
            Long userId = TokenUtil.parseUserId(token);
            if (userId == null) {
                log.warn("Token解析失败，拒绝访问: {}", uri);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Token解析失败\",\"code\":401}");
                return false;
            }

            log.info("Token验证通过 - 用户ID: {}, 访问: {}", userId, uri);
            
            // 将用户ID存储到request属性中，供后续Controller使用
            request.setAttribute("userId", userId);
            request.setAttribute("token", token);
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            // 请求完成后的处理（可选）
        }
    }
}
