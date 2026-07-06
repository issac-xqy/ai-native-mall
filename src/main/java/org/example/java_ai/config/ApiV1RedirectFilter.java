package org.example.java_ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 向后兼容：将旧 /api/* 请求 307 重定向到 /api/v1/*
 * 确保老客户端不中断
 */
@Component
public class ApiV1RedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/") && !uri.startsWith("/api/v1/")) {
            String newUri = "/api/v1" + uri.substring(4);
            if (request.getQueryString() != null) {
                newUri += "?" + request.getQueryString();
            }
            response.sendRedirect(newUri);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
