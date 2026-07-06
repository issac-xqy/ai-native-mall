package org.example.java_ai.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器 — 为每个请求生成唯一 traceId 注入 MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String traceId = httpReq.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put("traceId", traceId);
        MDC.put("requestUri", httpReq.getRequestURI());
        MDC.put("requestMethod", httpReq.getMethod());
        String clientIp = httpReq.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpReq.getRemoteAddr();
        }
        MDC.put("clientIp", clientIp);
        httpResp.setHeader(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 1000) {
                MDC.put("slowRequestMs", String.valueOf(elapsed));
            }
            MDC.clear();
        }
    }
}
