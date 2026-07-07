package org.example.java_ai.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.config.AiSecurityConfig;
import org.example.java_ai.service.ai.ProductOperationService;
import org.example.java_ai.service.ai.SmartCustomerService;
import org.example.java_ai.util.DataMaskingUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final SmartCustomerService customerService;
    private final ProductOperationService productOperationService;
    private final AiSecurityConfig securityConfig;

    @PostMapping("/customer-service/ask")
    @SentinelResource(value = "ai-api", fallback = "aiServiceFallback")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String question = request.get("question");
        String sessionId = request.getOrDefault("sessionId", "default");
        String apiKey = request.getOrDefault("apiKey", "default");

        log.info("智能客服请求 - {}", DataMaskingUtil.maskUserInfo(userId, "", ""));

        // API Key 验证：仅当后端注册了白名单 key 时才校验
        if (securityConfig.isApiKeyCheckEnabled() && !securityConfig.validateApiKey(apiKey)) {
            log.warn("API Key 校验失败: {}", apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null");
        }
        if (!securityConfig.checkRateLimit(userId, apiKey)) {
            return ResponseEntity.status(429).body(Map.of("error", "请求过于频繁，请稍后重试"));
        }

        String filteredQuestion = securityConfig.filterSensitiveWords(question);
        String answer = customerService.answerQuestion(userId, filteredQuestion, sessionId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "answer", answer,
            "sessionId", sessionId
        ));
    }

    @GetMapping(value = "/customer-service/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SentinelResource(value = "ai-api", fallback = "streamAiFallback")
    public Flux<String> streamAsk(@RequestParam String userId,
                                   @RequestParam String question,
                                   @RequestParam(required = false) String apiKey) {

        // API Key 验证：仅当后端注册了白名单 key 时才校验
        if (securityConfig.isApiKeyCheckEnabled() && !securityConfig.validateApiKey(apiKey)) {
            return Flux.just("data: {\"error\":\"无效的API Key\"}\n\n", "data: [DONE]\n\n");
        }
        if (!securityConfig.checkRateLimit(userId, apiKey)) {
            return Flux.just("data: {\"error\":\"请求过于频繁\"}\n\n", "data: [DONE]\n\n");
        }

        String filteredQuestion = securityConfig.filterSensitiveWords(question);

        return customerService.streamAnswer(userId, filteredQuestion)
                .onErrorResume(error -> {
                    log.error("流式响应错误", error);
                    return Flux.just(
                            "data: {\"error\":\"AI服务暂时不可用，请稍后重试\"}\n\n",
                            "data: [DONE]\n\n"
                    );
                });
    }

    @PostMapping("/product/seo-title")
    @SentinelResource(value = "ai-api", fallback = "aiServiceFallbackAsync")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateSeoTitle(
            @RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String category = request.get("category");
        String features = request.get("features");
        log.info("生成SEO标题 - 商品: {}", productName);
        return productOperationService.generateSeoTitle(productName, category, features)
                .thenApply(title -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "seoTitle", title
                )));
    }

    @PostMapping("/product/description")
    @SentinelResource(value = "ai-api", fallback = "aiServiceFallbackAsync")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateDescription(
            @RequestBody Map<String, Object> request) {
        String productName = (String) request.get("productName");
        @SuppressWarnings("unchecked")
        Map<String, String> specs = (Map<String, String>) request.get("specs");
        String targetAudience = (String) request.get("targetAudience");
        log.info("生成商品描述 - 商品: {}", productName);
        return productOperationService.generateProductDescription(productName, specs, targetAudience)
                .thenApply(description -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "description", description
                )));
    }

    @PostMapping("/comment/analyze")
    @SentinelResource(value = "ai-api", fallback = "aiServiceFallbackAsync")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> analyzeComment(
            @RequestBody Map<String, String> request) {
        String commentText = request.get("commentText");
        log.info("评论情感分析 - 评论长度: {}", commentText.length());
        return productOperationService.analyzeCommentSentiment(commentText)
                .thenApply(result -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "analysis", result
                )));
    }

    @PostMapping("/knowledge/import-products")
    public ResponseEntity<Map<String, Object>> importProductKnowledge(
            @RequestBody List<Map<String, String>> products) {
        log.info("批量导入商品知识 - 数量: {}", products.size());
        for (Map<String, String> product : products) {
            customerService.addProductKnowledge(
                product.get("productId"),
                product.get("productName"),
                product.get("description"),
                product.get("specs")
            );
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "商品知识导入任务已提交",
            "count", products.size()
        ));
    }

    @PostMapping("/product/reputation-report")
    @SentinelResource(value = "ai-api", fallback = "aiServiceFallbackAsync")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateReputationReport(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> comments = (List<String>) request.get("comments");
        log.info("生成口碑报告 - 评论数: {}", comments.size());
        return productOperationService.generateReputationReport(comments)
                .thenApply(report -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "report", report
                )));
    }

    /** Sentinel 限流/熔断 fallback — 同步接口 */
    public ResponseEntity<Map<String, Object>> aiServiceFallback(Map<String, String> request, Throwable t) {
        log.warn("AI 服务触发 Sentinel 限流/熔断: {}", t.getMessage());
        return ResponseEntity.status(503).body(Map.of(
            "success", false,
            "error", "AI服务繁忙，请稍后重试"
        ));
    }

    /** Sentinel fallback — 异步接口 */
    public CompletableFuture<ResponseEntity<Map<String, Object>>> aiServiceFallbackAsync(
            Object request, Throwable t) {
        log.warn("AI 异步服务触发 Sentinel 限流/熔断: {}", t.getMessage());
        return CompletableFuture.completedFuture(
            ResponseEntity.status(503).body(Map.of(
                "success", false,
                "error", "AI服务繁忙，请稍后重试"
            ))
        );
    }

    /** Sentinel fallback — 流式接口 */
    public Flux<String> streamAiFallback(String userId, String question, String apiKey, Throwable t) {
        log.warn("AI 流式服务触发 Sentinel 限流/熔断: {}", t.getMessage());
        return Flux.just(
            "data: {\"error\":\"AI服务繁忙，请稍后重试\"}\n\n",
            "data: [DONE]\n\n"
        );
    }
}
