package org.example.java_ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * AI能力层配置 - Spring AI + LangChain4j 双引擎
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@Configuration
public class AiCoreConfig {

    @Value("${spring.app.ai.api-key}")
    private String apiKey;

    @Value("${spring.app.ai.base-url}")
    private String baseUrl;

    @Value("${spring.app.ai.model}")
    private String modelName;

    @Value("${spring.app.ai.temperature}")
    private Double temperature;

    @Value("${spring.app.ai.max-tokens}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai-chat-model.timeout:PT120S}")
    private String timeoutDuration;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.app.ai.embedding-model}")
    private String embeddingModelName;

    @Value("${spring.app.ai.embedding-dimension}")
    private Integer embeddingDimension;

    @Value("${spring.app.ai.use-external-embedding:true}")
    private Boolean useExternalEmbedding;

    @Value("${spring.app.ai.auto-clear-vector-store:false}")
    private Boolean autoClearVectorStore;

    /**
     * 配置通义千问/DeepSeek Chat模型
     * 支持流式输出，用于智能客服、商品描述生成等场景
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化LangChain4j Chat模型: {}, URL: {}", modelName, baseUrl);
        log.info("API Key 前缀: {}", apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "null");
        
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))  // 增加到120秒，避免长文本处理超时
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    /**
     * 配置流式Chat模型（SSE）
     * 用于智能客服实时响应
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化LangChain4j Streaming Chat模型: {}", modelName);
        
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 配置嵌入模型（用于文本向量化）
     * 支持两种模式：
     * 1. 外部 Embedding 模型（默认）：使用通义千问 text-embedding-v4，通过 OpenAI 兼容接口调用
     * 2. 本地 ONNX 模型：AllMiniLmL6V2，无需网络，但效果略逊
     * 
     * @return EmbeddingModel
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if (useExternalEmbedding) {
            log.info("初始化外部 Embedding 模型: {}, 维度: {}", embeddingModelName, embeddingDimension);
            
            return OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(embeddingModelName)
                    .dimensions(embeddingDimension)  // 显式指定向量维度
                    .timeout(Duration.ofSeconds(60))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            log.info("初始化本地 ONNX Embedding 模型: AllMiniLmL6V2 (384维)");
            return new AllMiniLmL6V2EmbeddingModel();
        }
    }

    /**
     * 配置商品知识向量数据库 (使用Redis存储，支持持久化)
     * 使用 @Primary 标记为默认注入
     */
    @Bean("productEmbeddingStore")
    @org.springframework.context.annotation.Primary
    @Profile("!test")
    public EmbeddingStore<TextSegment> productEmbeddingStore() {
        log.info("初始化Redis商品知识向量数据库，维度: {}", embeddingDimension);
        
        RedisEmbeddingStore store = RedisEmbeddingStore.builder()
                .host(redisHost)
                .port(redisPort)
                .password(redisPassword != null && !redisPassword.isEmpty() ? redisPassword : null)
                .indexName("ai_mall_product_knowledge")  // 商品知识独立索引
                .dimension(embeddingDimension)
                .build();
        
        log.info("✅ Redis商品知识向量数据库初始化完成");
        return store;
    }

    /**
     * 配置FAQ知识向量数据库 (使用Redis存储，支持持久化)
     */
    @Bean("faqEmbeddingStore")
    @Profile("!test")
    public EmbeddingStore<TextSegment> faqEmbeddingStore() {
        log.info("初始化Redis FAQ知识向量数据库，维度: {}", embeddingDimension);
        
        RedisEmbeddingStore store = RedisEmbeddingStore.builder()
                .host(redisHost)
                .port(redisPort)
                .password(redisPassword != null && !redisPassword.isEmpty() ? redisPassword : null)
                .indexName("ai_mall_faq_knowledge")  // FAQ知识独立索引
                .dimension(embeddingDimension)
                .build();
        
        log.info("✅ Redis FAQ知识向量数据库初始化完成");
        return store;
    }

    /**
     * 配置RAG内容检索器 (默认使用商品知识库)
     */
    @Bean
    public ContentRetriever contentRetriever(@org.springframework.beans.factory.annotation.Qualifier("productEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
                                            EmbeddingModel embeddingModel) {
        log.info("初始化RAG内容检索器");
        
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();
    }
}
