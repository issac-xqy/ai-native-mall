package org.example.java_ai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 商品运营AI服务 - SEO标题生成、商品描述、评论情感分析、关联推荐
 */
@Slf4j
@Service
public class ProductOperationService {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public ProductOperationService(ChatLanguageModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成SEO友好的商品标题
     * 
     * @param productName 商品名称
     * @param category 商品分类
     * @param features 商品特点（逗号分隔）
     * @return SEO标题
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<String> generateSeoTitle(String productName, 
                                                       String category,
                                                       String features) {
        log.info("生成SEO标题 - 商品: {}", productName);
        
        String prompt = String.format("""
            你是一位专业的电商SEO专家。请为以下商品生成一个SEO友好的标题。
            
            要求：
            1. 包含核心关键词，利于搜索引擎收录
            2. 突出商品卖点和特色
            3. 长度控制在30-50字
            4. 符合用户搜索习惯
            5. 避免夸大宣传
            
            商品信息：
            - 商品名称：%s
            - 商品分类：%s
            - 商品特点：%s
            
            请直接输出优化后的标题，不要有其他说明。
            """, productName, category, features);
        
        try {
            String seoTitle = chatModel.generate(prompt);
            log.info("SEO标题生成成功: {}", seoTitle);
            return CompletableFuture.completedFuture(seoTitle.trim());
        } catch (Exception e) {
            log.error("SEO标题生成失败", e);
            return CompletableFuture.completedFuture(productName);
        }
    }

    /**
     * 生成商品详情描述
     * 
     * @param productName 商品名称
     * @param specs 商品规格参数
     * @param targetAudience 目标人群
     * @return 营销文案
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<String> generateProductDescription(String productName,
                                                                 Map<String, String> specs,
                                                                 String targetAudience) {
        log.info("生成商品描述 - 商品: {}", productName);
        
        StringBuilder specsStr = new StringBuilder();
        specs.forEach((key, value) -> specsStr.append("- ").append(key).append(": ").append(value).append("\n"));
        
        String prompt = String.format("""
            你是一位资深的电商文案策划师。请为以下商品撰写吸引人的详情描述。
            
            要求：
            1. 突出商品核心卖点和优势
            2. 结合目标人群需求，直击痛点
            3. 语言生动有感染力，但不过度夸张
            4. 结构清晰：引入 → 卖点介绍 → 使用场景 → 购买理由
            5. 字数控制在300-500字
            
            商品信息：
            - 商品名称：%s
            - 规格参数：
            %s
            - 目标人群：%s
            
            请直接输出商品描述文案。
            """, productName, specsStr, targetAudience);
        
        try {
            String description = chatModel.generate(prompt);
            log.info("商品描述生成成功，长度: {}", description.length());
            return CompletableFuture.completedFuture(description.trim());
        } catch (Exception e) {
            log.error("商品描述生成失败", e);
            return CompletableFuture.completedFuture("商品详情加载中...");
        }
    }

    /**
     * 评论情感分析与标签提取
     * 
     * @param commentText 评论文本
     * @return 情感分析结果（情感倾向 + 标签列表）
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<Map<String, Object>> analyzeCommentSentiment(String commentText) {
        log.info("分析评论情感 - 评论长度: {}", commentText.length());
        
        String prompt = String.format("""
            你是一位专业的用户评论分析师。请分析以下商品评论的情感倾向和关键标签。
            
            要求：
            1. 判断情感倾向：positive（正面）、negative（负面）、neutral（中性）
            2. 提取关键标签，如：质量好、物流快、性价比高、包装精美、质量差、物流慢、客服态度差等
            3. 以JSON格式返回结果
            
            评论文本：%s
            
            返回格式示例：
            {
              "sentiment": "positive",
              "tags": ["质量好", "物流快", "性价比高"],
              "summary": "用户对商品质量和物流速度表示满意"
            }
            
            请直接输出JSON，不要有其他说明。
            """, commentText);
        
        try {
            String result = chatModel.generate(prompt);
            log.info("评论情感分析完成 - AI返回结果: {}", result);
            
            // 解析AI返回的JSON
            try {
                // 清理可能的Markdown代码块标记
                String cleanJson = result.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("```$", "")
                    .trim();
                
                JsonNode rootNode = objectMapper.readTree(cleanJson);
                
                String sentiment = rootNode.has("sentiment") ? rootNode.get("sentiment").asText() : "neutral";
                String summary = rootNode.has("summary") ? rootNode.get("summary").asText() : "分析完成";
                
                // 解析tags数组
                String[] tags = new String[0];
                if (rootNode.has("tags") && rootNode.get("tags").isArray()) {
                    JsonNode tagsNode = rootNode.get("tags");
                    tags = StreamSupport.stream(tagsNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .toArray(String[]::new);
                }
                
                log.info("情感分析结果 - 情感: {}, 标签数: {}", sentiment, tags.length);
                
                Map<String, Object> analysis = Map.of(
                    "sentiment", sentiment,
                    "tags", tags,
                    "summary", summary
                );
                
                return CompletableFuture.completedFuture(analysis);
            } catch (Exception parseEx) {
                log.error("解析AI返回的JSON失败，使用原始结果", parseEx);
                // 如果解析失败，返回原始结果
                return CompletableFuture.completedFuture(Map.of(
                    "sentiment", "neutral",
                    "tags", new String[]{},
                    "summary", result
                ));
            }
        } catch (Exception e) {
            log.error("评论情感分析失败", e);
            return CompletableFuture.completedFuture(Map.of(
                "sentiment", "neutral",
                "tags", new String[]{},
                "summary", "分析失败"
            ));
        }
    }

    /**
     * 批量分析评论，生成商品口碑报告
     * 
     * @param comments 评论列表
     * @return 口碑报告
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<Map<String, Object>> generateReputationReport(java.util.List<String> comments) {
        log.info("生成商品口碑报告 - 评论数: {}", comments.size());
        
        StringBuilder allComments = new StringBuilder();
        for (int i = 0; i < Math.min(comments.size(), 50); i++) { // 最多分析50条
            allComments.append(i + 1).append(". ").append(comments.get(i)).append("\n");
        }
        
        String prompt = String.format("""
            你是电商数据分析专家。请基于以下用户评论，生成商品口碑分析报告。
            
            要求：
            1. 统计整体满意度（百分比，0-100）
            2. 提取高频好评标签（Top 5）
            3. 提取高频差评标签（Top 5）
            4. 给出改进建议
            5. 必须且只能返回纯JSON格式，不要包含任何Markdown或其他文本
            
            用户评论：
            %s
            
            返回格式：
            {
              "satisfaction_rate": 85.5,
              "positive_tags": ["质量好", "物流快"],
              "negative_tags": ["包装差", "客服慢"],
              "improvement_suggestions": ["建议改进包装", "提升客服响应速度"]
            }
            """, allComments);
        
        try {
            log.info("开始调用AI模型生成口碑报告...");
            String reportText = chatModel.generate(prompt);
            log.info("口碑报告生成成功, 报告长度: {}", reportText.length());
            log.info("📄 AI原始返回内容: {}", reportText);
            
            // 解析 JSON 结果
            Map<String, Object> result = new java.util.HashMap<>();
            try {
                // 提取 JSON 部分（去除可能的 Markdown 代码块标记）
                String jsonStr = reportText.replaceAll("^```(?:json)?\\s*", "")
                                           .replaceAll("\\s*```$", "");
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> reportData = mapper.readValue(jsonStr, Map.class);
                
                log.info("✅ JSON解析成功, 字段: {}", reportData.keySet());
                log.info("📊 satisfaction_rate={}, positive_tags={}, negative_tags={}",
                        reportData.get("satisfaction_rate"),
                        reportData.get("positive_tags"),
                        reportData.get("negative_tags"));
                
                result.put("satisfaction_rate", reportData.getOrDefault("satisfaction_rate", 0));
                result.put("positive_tags", reportData.getOrDefault("positive_tags", java.util.List.of()));
                result.put("negative_tags", reportData.getOrDefault("negative_tags", java.util.List.of()));
                result.put("improvement_suggestions", reportData.getOrDefault("improvement_suggestions", java.util.List.of()));
            } catch (Exception jsonEx) {
                log.error("❌ AI返回结果非标准JSON，解析失败 - 错误: {}", jsonEx.getMessage());
                log.error("📄 AI原始返回内容:\n{}", reportText);
                result.put("satisfaction_rate", 0);
                result.put("positive_tags", java.util.List.of());
                result.put("negative_tags", java.util.List.of());
                result.put("improvement_suggestions", java.util.List.of());
                result.put("raw_report", reportText);
            }
            
            result.put("total_comments", comments.size());
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("口碑报告生成失败 - 错误类型: {}, 错误信息: {}", 
                     e.getClass().getSimpleName(), e.getMessage());
            log.error("失败原因详情: ", e);
            
            // 根据异常类型提供更友好的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("timeout")) {
                errorMsg = "AI服务响应超时，请稍后重试或减少评论数量";
            } else if (errorMsg != null && errorMsg.contains("Canceled")) {
                errorMsg = "AI请求被取消，可能是网络不稳定或服务繁忙";
            }
            
            return CompletableFuture.completedFuture(Map.of(
                "satisfaction_rate", 0,
                "positive_tags", java.util.List.of(),
                "negative_tags", java.util.List.of(),
                "improvement_suggestions", java.util.List.of(),
                "error", errorMsg != null ? errorMsg : "未知错误"
            ));
        }
    }
}
