package org.example.java_ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.ProductComment;
import org.example.java_ai.mapper.ProductCommentMapper;
import org.example.java_ai.service.ProductCommentService;
import org.example.java_ai.service.ai.ProductOperationService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 商品评论服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl extends ServiceImpl<ProductCommentMapper, ProductComment> implements ProductCommentService {

    private final ProductOperationService productOperationService;

    @Override
    public Page<ProductComment> listComments(Long productId, Integer pageNum, Integer pageSize) {
        Page<ProductComment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductComment> wrapper = new LambdaQueryWrapper<ProductComment>()
            .eq(ProductComment::getProductId, productId)
            .orderByDesc(ProductComment::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public ProductComment addComment(ProductComment comment) {
        log.info("💾 保存评论 - productId: {}, userId: {}, content: {}", 
                comment.getProductId(), comment.getUserId(), comment.getContent());
        save(comment);
        log.info("✅ 评论保存成功，ID: {}, userId: {}", comment.getId(), comment.getUserId());
        return comment;
    }

    @Override
    public Map<String, Object> analyzeCommentSentiment(Long commentId) {
        ProductComment comment = getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        try {
            CompletableFuture<Map<String, Object>> future = 
                productOperationService.analyzeCommentSentiment(comment.getContent());
            Map<String, Object> result = future.get();
            
            // 更新评论的情感分析结果
            comment.setSentiment((String) result.get("sentiment"));
            String[] tags = (String[]) result.get("tags");
            comment.setAiTags(String.join(",", tags));
            comment.setSummary((String) result.get("summary"));
            updateById(comment);
            
            log.info("AI分析评论情感成功: {}", result);
            return result;
        } catch (Exception e) {
            log.error("分析评论情感失败", e);
            return Map.of("error", "分析失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteComment(Long commentId, Long userId) {
        ProductComment comment = getById(commentId);
        if (comment == null) {
            log.warn("评论不存在，ID: {}", commentId);
            return false;
        }
        
        if (!comment.getUserId().equals(userId)) {
            log.warn("用户 {} 无权删除评论 {} (作者: {})", userId, commentId, comment.getUserId());
            return false;
        }
        
        boolean deleted = removeById(commentId);
        if (deleted) {
            log.info("用户 {} 删除评论 {} 成功", userId, commentId);
        }
        return deleted;
    }
}
