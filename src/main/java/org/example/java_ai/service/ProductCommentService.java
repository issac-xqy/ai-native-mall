package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.java_ai.entity.ProductComment;

import java.util.Map;

/**
 * 商品评论服务接口
 */
public interface ProductCommentService extends IService<ProductComment> {

    /**
     * 分页查询评论
     */
    Page<ProductComment> listComments(Long productId, Integer pageNum, Integer pageSize);

    /**
     * 添加评论
     */
    ProductComment addComment(ProductComment comment);

    /**
     * AI分析单条评论情感
     */
    Map<String, Object> analyzeCommentSentiment(Long commentId);

    /**
     * 删除评论（验证权限）
     * @param commentId 评论ID
     * @param userId 当前用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);
}
