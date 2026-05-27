package org.example.java_ai.service.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.java_ai.aspect.AiMonitorAspect;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmartCustomerService AI 单元测试")
class SmartCustomerServiceTest {

    @Mock private ChatLanguageModel chatModel;
    @Mock private EmbeddingModel embeddingModel;
    @Mock private EmbeddingStore<TextSegment> productEmbeddingStore;
    @Mock private EmbeddingStore<TextSegment> faqEmbeddingStore;
    @Mock private ContentRetriever contentRetriever;
    @Mock private AiMonitorAspect aiMonitorAspect;

    private SmartCustomerService service;

    @BeforeEach
    void setUp() {
        service = new SmartCustomerService(
                chatModel, null, embeddingModel,
                productEmbeddingStore, faqEmbeddingStore,
                contentRetriever, aiMonitorAspect);
    }

    @Test
    @DisplayName("answerQuestion-检索到相关知识-返回AI回复")
    void answerQuestion_WithKnowledge_ReturnsAiResponse() {
        // 模拟向量检索结果
        TextSegment segment = TextSegment.from("iPhone 15 Pro 价格是 7999 元起");
        Content content = Content.from(segment);
        when(contentRetriever.retrieve(any())).thenReturn(List.of(content));

        // 模拟 Embedding（FAQ 检索需要）
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(
                new dev.langchain4j.data.embedding.Embedding(new float[1536])));

        // 模拟 FAQ 检索返回空
        when(faqEmbeddingStore.search(any())).thenReturn(
                new dev.langchain4j.store.embedding.EmbeddingSearchResult<>(List.of()));

        // 模拟大模型回复
        when(chatModel.generate(anyString()))
                .thenReturn("您好！根据知识库，iPhone 15 Pro 的价格是 7999 元起。");

        String answer = service.answerQuestion("user1", "iPhone 15 Pro 多少钱？", "session1");

        assertNotNull(answer);
        assertTrue(answer.contains("7999"));
        verify(aiMonitorAspect).recordAiCall(anyString(), anyString(), anyString(),
                anyString(), anyLong(), eq(false), isNull(), eq(false));
    }

    @Test
    @DisplayName("answerQuestion-知识库为空-仍返回AI回复")
    void answerQuestion_EmptyKnowledge_StillResponds() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(
                new dev.langchain4j.data.embedding.Embedding(new float[1536])));
        when(faqEmbeddingStore.search(any())).thenReturn(
                new dev.langchain4j.store.embedding.EmbeddingSearchResult<>(List.of()));
        when(chatModel.generate(anyString())).thenReturn("抱歉，当前知识库中没有相关信息");

        String answer = service.answerQuestion("user1", "今天天气怎么样？", "session2");

        assertNotNull(answer);
        assertTrue(answer.contains("知识库"));
        // 验证检索结果为空被记录
        verify(aiMonitorAspect).recordAiCall(anyString(), anyString(), anyString(),
                anyString(), anyLong(), eq(false), isNull(), eq(true));
    }

    @Test
    @DisplayName("answerQuestion-大模型异常-向上抛出")
    void answerQuestion_ChatModelException_Propagates() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(
                new dev.langchain4j.data.embedding.Embedding(new float[1536])));
        when(faqEmbeddingStore.search(any())).thenReturn(
                new dev.langchain4j.store.embedding.EmbeddingSearchResult<>(List.of()));
        when(chatModel.generate(anyString())).thenThrow(new RuntimeException("API 超时"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.answerQuestion("user1", "测试问题", "session3"));

        assertTrue(ex.getMessage().contains("API 超时"));
        // 验证异常被记录（异常路径 isMissingKnowledge 硬编码为 false）
        verify(aiMonitorAspect).recordAiCall(anyString(), anyString(), anyString(),
                isNull(), anyLong(), eq(true), eq("RuntimeException"), eq(false));
    }

    @Test
    @DisplayName("addProductKnowledge-正常添加-调用 Embedding 和存储")
    void addProductKnowledge_Normal_EmbedsAndStores() {
        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(
                new dev.langchain4j.data.embedding.Embedding(new float[1536])));

        service.addProductKnowledge("prod_001", "iPhone 15", "苹果手机", "128GB");

        verify(embeddingModel).embed(any(TextSegment.class));
        verify(productEmbeddingStore).add(any(), any(TextSegment.class));
    }

    @Test
    @DisplayName("importPolicyKnowledge-批量导入-逐条向量化")
    void importPolicyKnowledge_Batch_EmbedsEach() {
        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(
                new dev.langchain4j.data.embedding.Embedding(new float[1536])));

        service.importPolicyKnowledge(List.of("7天无理由退货", "15天包换"));

        verify(embeddingModel, times(2)).embed(any(TextSegment.class));
        verify(faqEmbeddingStore, times(2)).add(any(), any(TextSegment.class));
    }
}
