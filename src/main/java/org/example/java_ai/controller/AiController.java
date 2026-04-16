package org.example.java_ai.controller;

import org.example.java_ai.config.AiSecurityConfig;
import org.example.java_ai.service.ai.ProductOperationService;
import org.example.java_ai.service.ai.SmartCustomerService;
import org.example.java_ai.util.DataMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI能力API控制器
 * 
 * 提供智能客服、商品运营等AI能力接口
 * 所有接口均支持虚拟线程高并发处理
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final SmartCustomerService customerService;
    private final ProductOperationService productOperationService;
    private final AiSecurityConfig securityConfig;

    /**
     * 智能客服问答接口
     * 
     * @param request 请求参数 {userId, question, sessionId}
     * @return AI回复
     */
    @PostMapping("/customer-service/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String question = request.get("question");
        String sessionId = request.getOrDefault("sessionId", "default");
        String apiKey = request.get("apiKey");
        
        log.info("智能客服请求 - {}", DataMaskingUtil.maskUserInfo(userId, "", ""));
        
        // 1. 安全校验
        if (!securityConfig.validateApiKey(apiKey)) {
            return ResponseEntity.status(401).body(Map.of("error", "无效的API Key"));
        }
        
        // 2. 限流检查
        if (!securityConfig.checkRateLimit(userId, apiKey)) {
            return ResponseEntity.status(429).body(Map.of("error", "请求过于频繁，请稍后重试"));
        }
        
        // 3. 敏感词过滤
        String filteredQuestion = securityConfig.filterSensitiveWords(question);
        
        // 4. 调用AI服务
        String answer = customerService.answerQuestion(userId, filteredQuestion, sessionId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "answer", answer,
            "sessionId", sessionId
        ));
    }

    /**
     * 智能客服流式响应（SSE）
     * 前端可实现打字机效果
     * 
     * @param userId 用户ID
     * @param question 问题
     * @return 流式响应
     */
    @GetMapping(value = "/customer-service/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAsk(@RequestParam String userId, 
                                   @RequestParam String question,
                                   @RequestParam(required = false, defaultValue = "sk-test") String apiKey) {
        log.info("智能客服流式请求 - 用户: {}, 问题: {}", userId, question);
        
        // 安全校验
        if (!securityConfig.validateApiKey(apiKey)) {
            return Flux.just("data: {\"error\":\"无效的API Key\"}\n\n");
        }
        
        if (!securityConfig.checkRateLimit(userId, apiKey)) {
            return Flux.just("data: {\"error\":\"请求过于频繁\"}\n\n");
        }
        
        // 敏感词过滤
        String filteredQuestion = securityConfig.filterSensitiveWords(question);
        
        try {
            // 调用流式AI服务
            log.info("开始调用 AI 流式模型...");
            return customerService.streamAnswer(userId, filteredQuestion)
                    .map(token -> token)  // 直接返回token
                    .onErrorResume(error -> {
                        log.error("流式响应错误", error);
                        return Flux.just("抱歉，AI 服务暂时不可用: " + error.getMessage());
                    });
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            return Flux.just("抱歉，AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 生成SEO商品标题
     * 
     * @param request {productName, category, features}
     * @return SEO标题
     */
    @PostMapping("/product/seo-title")
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

    /**
     * 生成商品详情描述
     * 
     * @param request {productName, specs(JSON), targetAudience}
     * @return 商品描述
     */
    @PostMapping("/product/description")
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

    /**
     * 评论情感分析
     * 
     * @param request {commentText}
     * @return 情感分析结果
     */
    @PostMapping("/comment/analyze")
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

    /**
     * 批量导入商品知识到向量库
     * 
     * @param products 商品列表
     * @return 导入结果
     */
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

    /**
     * 生成商品口碑报告
     * 
     * @param request {comments: [...]}
     * @return 口碑报告
     */
    @PostMapping("/product/reputation-report")
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
}
