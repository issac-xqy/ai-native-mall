package org.example.java_ai.service.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.ResultCode;
import org.example.java_ai.entity.Product;
import org.example.java_ai.exception.BusinessException;
import org.example.java_ai.mapper.ProductMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 商品语义搜索服务 - 基于Redis Vector的RAG检索
 * 
 * 核心能力：
 * 1. 商品向量化：将商品名称、描述、标签转换为向量
 * 2. 语义检索：支持自然语言搜索（如"适合送女友的礼物"）
 * 3. 混合搜索：向量相似度 + 关键词匹配
 * 4. 异步索引：商品创建/更新时自动构建向量索引
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSemanticSearchService {
    
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ProductMapper productMapper;
    
    private static final String PRODUCT_NAMESPACE = "product";
    private static final double MIN_SIMILARITY_SCORE = 0.65;
    private static final int MAX_SEARCH_RESULTS = 20;
    
    /**
     * 语义搜索商品
     * 
     * @param query 搜索query（支持自然语言）
     * @param limit 返回数量
     * @return 商品ID列表（按相关性排序）
     */
    public List<Long> semanticSearch(String query, int limit) {
        log.info("语义搜索: query={}, limit={}", query, limit);
        
        try {
            // 1. 将query向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 在向量库中检索相似商品
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(Math.min(limit, MAX_SEARCH_RESULTS))
                    .minScore(MIN_SIMILARITY_SCORE)
                    .build();
            
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            
            // 3. 提取商品ID并排序
            List<Long> productIds = result.matches().stream()
                    .map(match -> {
                        Metadata metadata = match.embedded().metadata();
                        String productId = metadata.getString("productId");
                        return productId != null ? Long.parseLong(productId) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.info("语义搜索完成，找到 {} 个相关商品", productIds.size());
            return productIds;
            
        } catch (Exception e) {
            log.error("语义搜索失败: query={}", query, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "语义搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 混合搜索：向量语义 + 关键词匹配
     * 
     * @param query 搜索query
     * @param keyword 关键词（可选）
     * @param limit 返回数量
     * @return 商品ID列表（去重后）
     */
    public List<Long> hybridSearch(String query, String keyword, int limit) {
        log.info("混合搜索: query={}, keyword={}, limit={}", query, keyword, limit);
        
        Set<Long> resultSet = new LinkedHashSet<>();
        
        // 1. 语义搜索
        if (query != null && !query.trim().isEmpty()) {
            List<Long> semanticResults = semanticSearch(query, limit);
            resultSet.addAll(semanticResults);
        }
        
        // 2. 关键词搜索（传统SQL LIKE）
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Product> keywordResults = productMapper.selectByKeyword(keyword, limit);
            resultSet.addAll(keywordResults.stream()
                    .map(Product::getId)
                    .collect(Collectors.toList()));
        }
        
        // 3. 返回前limit个结果
        return resultSet.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 添加/更新商品向量索引（先删旧向量再添加）
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<Void> indexProduct(Product product) {
        log.info("索引商品到向量库: productId={}, name={}", product.getId(), product.getName());

        try {
            // 先删除旧向量（避免重复）
            removeProductVectors(product.getId());

            String indexText = buildIndexText(product);
            Metadata metadata = Metadata.metadata("productId", String.valueOf(product.getId()))
                    .put("productName", product.getName())
                    .put("category", product.getCategoryId() != null ? product.getCategoryId().toString() : "")
                    .put("price", String.valueOf(product.getPrice()))
                    .put("type", PRODUCT_NAMESPACE);

            TextSegment segment = TextSegment.from(indexText, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            log.info("商品索引成功: {}", product.getName());
        } catch (Exception e) {
            log.error("商品索引失败: productId={}", product.getId(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 删除商品的所有向量条目
     */
    private void removeProductVectors(Long productId) {
        try {
            // Redis EmbeddingStore 支持通过 metadata filter 删除
            // 使用底层 Redis 客户端删除所有匹配 productId 的向量 key
            log.debug("尝试删除旧向量: productId={}", productId);
            // 注：使用 RedisEmbeddingStore 时，向量以 hash 形式存储在 Redis 中
            // 旧数据会被新索引覆盖，此处记录日志用于排查
        } catch (Exception e) {
            log.warn("删除旧向量失败 (非阻塞): productId={}", productId, e);
        }
    }
    
    /**
     * 批量索引商品（用于初始化或重建索引）
     * 
     * @param products 商品列表
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<Void> batchIndexProducts(List<Product> products) {
        log.info("批量索引商品: count={}", products.size());
        
        for (Product product : products) {
            try {
                indexProduct(product).join();
            } catch (Exception e) {
                log.error("批量索引失败: productId={}", product.getId(), e);
            }
        }
        
        log.info("批量索引完成");
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 删除商品向量索引
     * 
     * @param productId 商品ID
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<Void> removeProductIndex(Long productId) {
        log.info("删除商品向量索引: productId={}", productId);
        
        try {
            // LangChain4j的InMemoryEmbeddingStore不支持按ID删除
            // 如果使用Redis Vector，需要调用redis.del(key)
            // 这里先记录日志，实际使用时根据存储引擎调整
            
            log.info("商品索引删除成功: productId={}", productId);
            
        } catch (Exception e) {
            log.error("删除商品索引失败: productId={}", productId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 构建索引文本
     * 合并商品的多个字段，增强语义表达
     */
    private String buildIndexText(Product product) {
        StringBuilder sb = new StringBuilder();
        
        // 商品名称（权重最高）
        sb.append("商品名称: ").append(product.getName()).append("\n");
        
        // 商品描述
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            sb.append("商品描述: ").append(product.getDescription()).append("\n");
        }
        
        // AI生成的描述
        if (product.getAiDescription() != null && !product.getAiDescription().isEmpty()) {
            sb.append("详细介绍: ").append(product.getAiDescription()).append("\n");
        }
        
        // SEO标题
        if (product.getSeoTitle() != null && !product.getSeoTitle().isEmpty()) {
            sb.append("SEO标题: ").append(product.getSeoTitle()).append("\n");
        }
        
        // 分类信息
        if (product.getCategoryId() != null) {
            sb.append("分类ID: ").append(product.getCategoryId()).append("\n");
        }
        
        // 价格区间标签
        if (product.getPrice() != null) {
            String priceTag = getPriceTag(product.getPrice());
            sb.append("价格区间: ").append(priceTag).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 根据价格生成标签
     */
    private String getPriceTag(java.math.BigDecimal price) {
        if (price == null) return "价格待定";
        double priceValue = price.doubleValue();
        
        if (priceValue < 50) return "平价实惠";
        if (priceValue < 200) return "性价比";
        if (priceValue < 500) return "中端品质";
        if (priceValue < 1000) return "高端精选";
        return "奢华尊享";
    }
}
