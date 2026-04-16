package org.example.java_ai.service.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.aspect.AiMonitorAspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 智能客服服务 - 基于RAG的语义问答
 * 
 * 核心能力：
 * 1. 向量知识库检索：从Redis向量库中检索相关商品/政策知识
 * 2. RAG增强生成：结合检索结果，生成准确的客服回复
 * 3. 流式响应：SSE实时输出，首字返回 < 1.5秒
 * 4. 上下文记忆：支持多轮对话
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Slf4j
@Service
public class SmartCustomerService {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> productEmbeddingStore;
    private final EmbeddingStore<TextSegment> faqEmbeddingStore;
    private final ContentRetriever contentRetriever;
    private final AiMonitorAspect aiMonitorAspect;

    public SmartCustomerService(
            ChatLanguageModel chatModel,
            StreamingChatLanguageModel streamingChatModel,
            EmbeddingModel embeddingModel,
            @Qualifier("productEmbeddingStore") EmbeddingStore<TextSegment> productEmbeddingStore,
            @Qualifier("faqEmbeddingStore") EmbeddingStore<TextSegment> faqEmbeddingStore,
            ContentRetriever contentRetriever,
            AiMonitorAspect aiMonitorAspect) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.embeddingModel = embeddingModel;
        this.productEmbeddingStore = productEmbeddingStore;
        this.faqEmbeddingStore = faqEmbeddingStore;
        this.contentRetriever = contentRetriever;
        this.aiMonitorAspect = aiMonitorAspect;
    }

    /**
     * 智能客服问答（同步）
     * 
     * @param userId 用户ID
     * @param question 用户问题
     * @param sessionId 会话ID
     * @return AI回复
     */
    public String answerQuestion(String userId, String question, String sessionId) {
        long startTime = System.currentTimeMillis();
        log.info("智能客服问答 - 用户: {}, 问题: {}", userId, question);
        
        try {
            // 1. RAG检索相关知识
            var relevantContent = contentRetriever.retrieve(new Query(question));
            log.info("检索到 {} 条相关知识", relevantContent.size());
            
            // 2. 额外检索FAQ知识库
            try {
                var testEmbedding = embeddingModel.embed(question).content();
                var faqResults = faqEmbeddingStore.search(
                    dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                        .queryEmbedding(testEmbedding)
                        .maxResults(3)
                        .minScore(0.5)
                        .build()
                );
                for (var match : faqResults.matches()) {
                    relevantContent.add(new dev.langchain4j.rag.content.Content(match.embedded()));
                }
                log.info("从FAQ知识库额外检索到 {} 条知识", faqResults.matches().size());
            } catch (Exception e) {
                log.warn("检索FAQ知识库失败: {}", e.getMessage());
            }
            
            // 3. 构建增强提示词
            String systemPrompt = buildSystemPrompt(relevantContent);
            
            // 4. 调用大模型生成回复
            String response = chatModel.generate(
                systemPrompt + "\n\n用户问题: " + question
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 4. 记录监控日志
            aiMonitorAspect.recordAiCall(
                sessionId, userId, question, response, responseTime,
                false, null, relevantContent.isEmpty()
            );
            
            log.info("AI回复生成完成，长度: {}, 耗时: {}ms", response.length(), responseTime);
            return response;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            aiMonitorAspect.recordAiCall(
                sessionId, userId, question, null, responseTime,
                true, e.getClass().getSimpleName(), false
            );
            throw e;
        }
    }

    /**
     * 智能客服流式响应（SSE）
     * 用于前端打字机效果展示
     * 
     * @param userId 用户ID
     * @param question 用户问题
     * @return 流式响应Flux
     */
    public Flux<String> streamAnswer(String userId, String question) {
        long startTime = System.currentTimeMillis();
        String sessionId = "session_" + System.currentTimeMillis();
        log.info("智能客服流式响应 - 用户: {}, 问题: {}", userId, question);
        
        StringBuilder fullAnswer = new StringBuilder();
        
        return Flux.<String>create(sink -> {
            try {
                // 1. RAG检索相关知识
                var relevantContent = contentRetriever.retrieve(new Query(question));
                log.info("检索到 {} 条相关知识", relevantContent.size());
                
                // 2. 额外检索FAQ知识库
                try {
                    var testEmbedding = embeddingModel.embed(question).content();
                    var faqResults = faqEmbeddingStore.search(
                        dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                            .queryEmbedding(testEmbedding)
                            .maxResults(3)
                            .minScore(0.5)
                            .build()
                    );
                    for (var match : faqResults.matches()) {
                        relevantContent.add(new dev.langchain4j.rag.content.Content(match.embedded()));
                    }
                    log.info("从FAQ知识库额外检索到 {} 条知识", faqResults.matches().size());
                } catch (Exception e) {
                    log.warn("检索FAQ知识库失败: {}", e.getMessage());
                }
                
                // 3. 构建增强提示词
                String systemPrompt = buildSystemPrompt(relevantContent);
                String fullPrompt = systemPrompt + "\n\n用户问题: " + question;
                
                // 4. 使用流式模型真正逐Token输出（AI边生成边返回）
                streamingChatModel.generate(fullPrompt, new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        fullAnswer.append(token);
                        // SSE格式：每行以data:开头，以\n\n结束
                        sink.next("data: " + token + "\n\n");
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        log.info("AI流式回复完成，长度: {}, 耗时: {}ms", fullAnswer.length(), responseTime);
                        
                        // 记录监控日志
                        aiMonitorAspect.recordAiCall(
                            sessionId, userId, question, fullAnswer.toString(), responseTime,
                            false, null, relevantContent.isEmpty()
                        );
                        
                        // SSE结束标记
                        sink.next("data: [DONE]\n\n");
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        log.error("流式响应异常", error);
                        
                        aiMonitorAspect.recordAiCall(
                            sessionId, userId, question, null, responseTime,
                            true, error.getClass().getSimpleName(), false
                        );
                        
                        sink.error(error);
                    }
                });
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                log.error("流式响应异常", e);
                
                aiMonitorAspect.recordAiCall(
                    sessionId, userId, question, null, responseTime,
                    true, e.getClass().getSimpleName(), false
                );
                
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 添加商品知识到向量库
     * 用于RAG检索增强
     * 
     * @param productId 商品ID
     * @param productName 商品名称
     * @param description 商品描述
     * @param specs 商品规格
     */
    public void addProductKnowledge(String productId, 
                                    String productName,
                                    String description,
                                    String specs) {
        log.info("添加商品知识到向量库 - 商品: {}", productName);
        
        try {
            // 1. 构建文档内容
            String content = String.format("""
                商品名称: %s
                商品描述: %s
                商品规格: %s
                """, productName, description, specs);
            
            // 2. 创建元数据
            Metadata metadata = Metadata.metadata("productId", productId)
                    .put("productName", productName)
                    .put("type", "product");
            
            // 3. 向量化并存入商品向量库
            TextSegment segment = TextSegment.from(content, metadata);
            var embedding = embeddingModel.embed(segment).content();
            productEmbeddingStore.add(embedding, segment);
            
            log.info("商品知识添加成功: {}", productName);
        } catch (Exception e) {
            log.error("添加商品知识失败: {}", productName, e);
        }
    }

    /**
     * 批量导入售后政策知识库
     */
    public void importPolicyKnowledge(List<String> policies) {
        log.info("批量导入售后政策知识，共 {} 条", policies.size());
        
        for (int i = 0; i < policies.size(); i++) {
            String policy = policies.get(i);
            
            Metadata metadata = Metadata.metadata("policyId", "policy_" + i)
                    .put("type", "policy");
            
            TextSegment segment = TextSegment.from(policy, metadata);
            var embedding = embeddingModel.embed(segment).content();
            faqEmbeddingStore.add(embedding, segment);
        }
        
        log.info("售后政策知识导入完成");
    }

    /**
     * 构建系统提示词（RAG增强）
     */
    private String buildSystemPrompt(List<dev.langchain4j.rag.content.Content> relevantContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            你是AI-Native智能商城的AI客服助手，名叫“小Q”，由谢卿勇搭建。你专业、友好、准确。
            当用户询问你是谁时，请回答：“您好！我是小Q,高兴为您服务。请问有什么可以帮助您的？”
            
            以下是相关的商品知识和售后政策（仅供参考）：
            """);
        
        if (relevantContent.isEmpty()) {
            prompt.append("暂无相关知识库记录。\n");
        } else {
            for (dev.langchain4j.rag.content.Content content : relevantContent) {
                prompt.append("- ").append(content.textSegment().text()).append("\n");
            }
        }
        
        prompt.append("""
            
            【回答准则】：
            1. 严格根据上述提供的知识库内容回答用户问题。
            2. 如果知识库中没有相关信息，请明确告知用户："抱歉，当前知识库中没有相关信息，建议您咨询人工客服。"，严禁编造答案。
            3. 回答要简洁明了，避免冗长。
            """);
        
        return prompt.toString();
    }
}
