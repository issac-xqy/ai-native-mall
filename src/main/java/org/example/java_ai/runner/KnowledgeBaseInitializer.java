package org.example.java_ai.runner;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.Product;
import org.example.java_ai.mapper.ProductMapper;
import org.example.java_ai.service.ai.SmartCustomerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI知识库初始化器
 * 应用启动时自动从数据库加载所有商品知识到向量库
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Slf4j
@Component
@Profile("!test")
public class KnowledgeBaseInitializer implements CommandLineRunner {

    private final ProductMapper productMapper;
    private final SmartCustomerService customerService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeBaseInitializer(
            ProductMapper productMapper,
            SmartCustomerService customerService,
            @Qualifier("productEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.productMapper = productMapper;
        this.customerService = customerService;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        log.info("🤖 开始初始化AI商品知识库...");
        
        try {
            // 等待8秒，让 VectorStoreDimensionChecker 和 FAQKnowledgeInitializer 先完成
            log.info("⏳ 等待向量维度检查和FAQ初始化完成...");
            Thread.sleep(8000);
            
            // 1. 检查Redis中是否已有商品知识向量
            if (hasProductKnowledge()) {
                log.info("✅ Redis中已存在商品知识库，跳过初始化（持久化生效）");
                return;
            }
            
            log.info("📦 Redis中无商品知识，开始构建向量索引...");
            
            // 2. 查询所有已上架商品
            List<Product> products = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                    .eq(Product::getStatus, 1)
                    .eq(Product::getDeleted, 0)
            );
            
            if (products.isEmpty()) {
                log.warn("⚠️ 未找到任何商品，知识库为空");
                return;
            }
            
            log.info("📦 发现 {} 个商品，正在构建向量索引...", products.size());
            
            // 3. 批量导入商品知识
            int successCount = 0;
            int failCount = 0;
            
            for (Product product : products) {
                try {
                    customerService.addProductKnowledge(
                        String.valueOf(product.getId()),
                        product.getName(),
                        product.getDescription() != null ? product.getDescription() : "",
                        product.getSpecs() != null ? product.getSpecs() : ""
                    );
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 商品知识导入失败: {}", product.getName(), e);
                    failCount++;
                }
            }
            
            log.info("✅ AI商品知识库初始化完成！成功: {} 个，失败: {} 个 (已持久化到Redis)", successCount, failCount);
            
        } catch (Exception e) {
            log.error("❌ AI知识库初始化失败", e);
        }
    }
    
    /**
     * 检查Redis中是否已有商品知识向量
     * 通过Redis索引列表判断
     */
    private boolean hasProductKnowledge() {
        try {
            var testEmbedding = embeddingModel.embed("test").content();
            var results = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                    .queryEmbedding(testEmbedding)
                    .maxResults(1)
                    .minScore(0.0)
                    .build()
            );
            
            boolean hasData = results.matches() != null && !results.matches().isEmpty();
            log.info("🔍 检查Redis商品知识库: {} (匹配数: {})", 
                hasData ? "已存在" : "不存在", 
                results.matches() != null ? results.matches().size() : 0);
            return hasData;
        } catch (Exception e) {
            log.warn("⚠️ 检查Redis商品知识库失败，将重新构建: {}", e.getMessage());
            return false;
        }
    }
}
