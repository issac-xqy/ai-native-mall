package org.example.java_ai.service.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.aspect.AiMonitorAspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    private static final int MAX_CONTEXT_CHARS = 4000;

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

    public String answerQuestion(String userId, String question, String sessionId) {
        long startTime = System.currentTimeMillis();
        log.info("智能客服问答 - 用户: {}, 问题: {}", userId, question);

        try {
            List<Content> relevantContent = retrieveRelevantContent(question);
            String systemPrompt = buildSystemPrompt(relevantContent);
            String response = chatModel.generate(systemPrompt + "\n\n用户问题: " + question);

            long responseTime = System.currentTimeMillis() - startTime;
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

    public Flux<String> streamAnswer(String userId, String question) {
        long startTime = System.currentTimeMillis();
        String sessionId = "session_" + System.currentTimeMillis();
        log.info("智能客服流式响应 - 用户: {}, 问题: {}", userId, question);

        StringBuilder fullAnswer = new StringBuilder();

        return Flux.<String>create(sink -> {
            try {
                List<Content> relevantContent = retrieveRelevantContent(question);
                String systemPrompt = buildSystemPrompt(relevantContent);
                String fullPrompt = systemPrompt + "\n\n用户问题: " + question;

                streamingChatModel.generate(fullPrompt, new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        fullAnswer.append(token);
                        sink.next("data: " + token + "\n\n");
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        aiMonitorAspect.recordAiCall(
                            sessionId, userId, question, fullAnswer.toString(), responseTime,
                            false, null, relevantContent.isEmpty()
                        );
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
                        sink.next("data: [DONE]\n\n");
                        sink.complete();
                    }
                });
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                log.error("流式响应异常", e);
                aiMonitorAspect.recordAiCall(
                    sessionId, userId, question, null, responseTime,
                    true, e.getClass().getSimpleName(), false
                );
                sink.next("data: [DONE]\n\n");
                sink.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 并行检索商品库 + FAQ 库，合并并截断到最大上下文长度
     */
    private List<Content> retrieveRelevantContent(String question) {
        // 并行：商品库 + FAQ 库
        CompletableFuture<List<Content>> productFuture =
                CompletableFuture.supplyAsync(() -> contentRetriever.retrieve(new Query(question)));
        CompletableFuture<List<Content>> faqFuture =
                CompletableFuture.supplyAsync(() -> searchFaqStore(question));

        List<Content> allContent = new ArrayList<>();
        try {
            allContent.addAll(productFuture.get());
        } catch (Exception e) {
            log.warn("商品库检索失败: {}", e.getMessage());
        }
        try {
            allContent.addAll(faqFuture.get());
        } catch (Exception e) {
            log.warn("FAQ库检索失败: {}", e.getMessage());
        }

        // 截断到 MAX_CONTEXT_CHARS
        int totalChars = 0;
        List<Content> trimmed = new ArrayList<>();
        for (Content c : allContent) {
            int len = c.textSegment().text().length();
            if (totalChars + len > MAX_CONTEXT_CHARS) break;
            trimmed.add(c);
            totalChars += len;
        }

        log.info("检索到 {} 条知识（截断后 {} 条，共 {} 字符）", allContent.size(), trimmed.size(), totalChars);
        return trimmed;
    }

    private List<Content> searchFaqStore(String question) {
        try {
            var embedding = embeddingModel.embed(question).content();
            var results = faqEmbeddingStore.search(
                EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(3)
                    .minScore(0.5)
                    .build()
            );
            return results.matches().stream()
                    .map(m -> new Content(m.embedded()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("FAQ库检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void addProductKnowledge(String productId, String productName,
                                    String description, String specs) {
        log.info("添加商品知识到向量库 - 商品: {}", productName);
        try {
            String content = String.format("""
                商品名称: %s
                商品描述: %s
                商品规格: %s
                """, productName, description, specs);

            var metadata = dev.langchain4j.data.document.Metadata.metadata("productId", productId)
                    .put("productName", productName)
                    .put("type", "product");

            TextSegment segment = TextSegment.from(content, metadata);
            var embedding = embeddingModel.embed(segment).content();
            productEmbeddingStore.add(embedding, segment);
            log.info("商品知识添加成功: {}", productName);
        } catch (Exception e) {
            log.error("添加商品知识失败: {}", productName, e);
        }
    }

    public void importPolicyKnowledge(List<String> policies) {
        log.info("批量导入售后政策知识，共 {} 条", policies.size());
        for (int i = 0; i < policies.size(); i++) {
            String policy = policies.get(i);
            var metadata = dev.langchain4j.data.document.Metadata.metadata("policyId", "policy_" + i)
                    .put("type", "policy");
            TextSegment segment = TextSegment.from(policy, metadata);
            var embedding = embeddingModel.embed(segment).content();
            faqEmbeddingStore.add(embedding, segment);
        }
        log.info("售后政策知识导入完成");
    }

    private String buildSystemPrompt(List<Content> relevantContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            你是AI-Native智能商城的AI客服助手，名叫“小Q”。你专业、友好、准确。
            当用户询问你是谁时，请回答：“您好！我是小Q，高兴为您服务。请问有什么可以帮助您的？”

            以下是相关的商品知识和售后政策（仅供参考）：
            """);

        if (relevantContent.isEmpty()) {
            prompt.append("暂无相关知识库记录。\n");
        } else {
            for (Content c : relevantContent) {
                prompt.append("- ").append(c.textSegment().text()).append("\n");
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
