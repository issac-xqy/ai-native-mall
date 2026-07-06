package org.example.java_ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * AI 配置 — 类型安全的 @ConfigurationProperties
 * 替代散落的 @Value 注入
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiConfigProperties {

    /** API Key（可通过 AI_API_KEY 环境变量覆盖） */
    private String apiKey = "";

    /** API 地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 模型名称 */
    private String model = "qwen-max";

    /** 温度参数 (0-2) */
    private double temperature = 0.7;

    /** 最大输出 token */
    private int maxTokens = 2000;

    /** Top-P 采样 */
    private double topP = 0.8;

    /** Embedding 模型 */
    private String embeddingModel = "text-embedding-v4";

    /** Embedding 维度 */
    private int embeddingDimension = 1536;

    /** 是否使用外部 Embedding */
    private boolean useExternalEmbedding = true;

    /** 向量维度不匹配时是否自动清理 */
    private boolean autoClearVectorStore = true;

    /** DeepSeek API Key */
    private String deepseekApiKey = "";

    /** DeepSeek API 地址 */
    private String deepseekBaseUrl = "https://api.deepseek.com/v1";

    /** DeepSeek 模型 */
    private String deepseekModel = "deepseek-chat";
}
