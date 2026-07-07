package org.example.java_ai.runner;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 向量存储维度检测器
 * 应用启动时自动检测 Redis 中向量维度是否匹配，不匹配时自动清理
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@Profile("!test")
public class VectorStoreDimensionChecker implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;
    
    @Qualifier("productEmbeddingStore")
    private final EmbeddingStore<TextSegment> productEmbeddingStore;
    
    @Qualifier("faqEmbeddingStore")
    private final EmbeddingStore<TextSegment> faqEmbeddingStore;
    
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.app.ai.embedding-dimension}")
    private Integer currentDimension;

    @Value("${spring.app.ai.auto-clear-vector-store:true}")
    private Boolean autoClearVectorStore;

    @Override
    public void run(String... args) {
        log.info("🔍 开始检查向量存储维度...");
        
        boolean needClear = false;
        
        // 检查商品知识库维度
        if (hasDimensionMismatch(productEmbeddingStore, "商品知识库", "ai_mall_product_knowledge")) {
            needClear = true;
        }
        
        // 检查 FAQ 知识库维度
        if (hasDimensionMismatch(faqEmbeddingStore, "FAQ知识库", "ai_mall_faq_knowledge")) {
            needClear = true;
        }
        
        if (needClear && autoClearVectorStore) {
            log.warn("⚠️ 检测到向量维度不匹配，开始自动清理...");
            clearVectorStore("ai_mall_product_knowledge", "商品知识库");
            clearVectorStore("ai_mall_faq_knowledge", "FAQ知识库");
            log.info("✅ 向量存储清理完成，请重新上传知识库文档");
        } else if (needClear && !autoClearVectorStore) {
            log.error("❌ 检测到向量维度不匹配，请手动清理 Redis 或设置 auto-clear-vector-store=true");
            log.error("💡 或者执行: redis-cli -p 6380 FLUSHDB");
        } else {
            log.info("✅ 向量存储维度检查通过");
        }
    }

    /**
     * 检查向量库维度是否匹配
     */
    private boolean hasDimensionMismatch(EmbeddingStore<TextSegment> store, String storeName, String indexName) {
        try {
            // 获取一个测试向量
            var testEmbedding = embeddingModel.embed("dimension check").content();
            int expectedDimension = testEmbedding.dimension();
            
            // 尝试搜索
            var results = store.search(
                EmbeddingSearchRequest.builder()
                    .queryEmbedding(testEmbedding)
                    .maxResults(1)
                    .minScore(0.0)
                    .build()
            );
            
            // 如果有数据，检查维度
            if (results.matches() != null && !results.matches().isEmpty()) {
                var firstMatch = results.matches().get(0);
                int actualDimension = firstMatch.embedding().dimension();
                
                if (actualDimension != expectedDimension) {
                    log.error("❌ {} 维度不匹配: 当前模型维度={}, Redis中向量维度={}", 
                        storeName, expectedDimension, actualDimension);
                    return true;
                }
                
                log.info("✅ {} 维度检查通过: {}维", storeName, actualDimension);
            } else {
                log.info("📭 {} 为空，无需检查", storeName);
            }
            
            return false;
        } catch (Exception e) {
            log.warn("⚠️ 检查 {} 维度失败: {}", storeName, e.getMessage());
            // 如果索引不存在，尝试创建
            if (e.getMessage().contains("Unknown index name")) {
                log.info("📭 {} 索引不存在，将在首次使用时创建", storeName);
                return false;
            }
            // 维度不匹配也会抛出异常，需要清理
            if (e.getMessage().contains("does not match index's expected size")) {
                log.error("❌ {} 检测到维度不匹配异常，需要清理", storeName);
                return true;
            }
            return false;
        }
    }

    /**
     * 清空指定的向量存储索引
     */
    private void clearVectorStore(String indexName, String storeName) {
        try {
            // 方法1: 使用 RediSearch 的 FT.DROPINDEX 命令删除整个索引（包括数据和元数据）
            try {
                redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                    connection.execute("FT.DROPINDEX", indexName.getBytes());
                    return "OK";
                });
                log.info("✅ 已删除 {} 索引及其所有数据", storeName);
            } catch (Exception e) {
                log.warn("⚠️ 删除索引失败（可能不存在）: {}", e.getMessage());
            }
            
            // 方法2: 清理残留的键（作为备份方案）
            String pattern = indexName + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.info("✅ 已清理 {} 的残留键，删除 {} 个", storeName, deletedCount);
            } else {
                log.info("📭 {} 无残留数据", storeName);
            }
        } catch (Exception e) {
            log.error("❌ 清空 {} 失败: {}", storeName, e.getMessage(), e);
        }
    }
}
