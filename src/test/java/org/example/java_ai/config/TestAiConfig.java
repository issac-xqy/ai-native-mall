package org.example.java_ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestAiConfig {

    @Bean("productEmbeddingStore")
    @Primary
    public EmbeddingStore<TextSegment> testProductEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean("faqEmbeddingStore")
    public EmbeddingStore<TextSegment> testFaqEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
