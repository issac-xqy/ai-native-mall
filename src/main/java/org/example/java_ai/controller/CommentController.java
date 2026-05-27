package org.example.java_ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.ProductComment;
import org.example.java_ai.service.ProductCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ProductCommentService commentService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 分页查询商品评论
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> listComments(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("📥 收到评论查询 - 商品ID: {}, 页码: {}", productId, pageNum);
        Page<ProductComment> page = commentService.listComments(productId, pageNum, pageSize);
        
        // 为每条评论添加用户信息
        List<Map<String, Object>> commentsWithUser = page.getRecords().stream()
            .map(comment -> {
                Map<String, Object> commentMap = new java.util.HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("productId", comment.getProductId());
                commentMap.put("userId", comment.getUserId());
                commentMap.put("content", comment.getContent());
                commentMap.put("rating", comment.getRating());
                commentMap.put("sentiment", comment.getSentiment());
                commentMap.put("aiTags", comment.getAiTags());
                commentMap.put("summary", comment.getSummary());
                
                // 格式化时间为字符串（ISO 8601 格式）
                if (comment.getCreateTime() != null) {
                    commentMap.put("createTime", comment.getCreateTime().toString());
                    log.debug("📅 评论ID {} 的 createTime: {}", comment.getId(), comment.getCreateTime());
                } else {
                    commentMap.put("createTime", null);
                    log.warn("⚠️ 评论ID {} 的 createTime 为 null", comment.getId());
                }
                
                // 查询用户信息
                try {
                    String userSql = "SELECT username, nickname FROM sys_user WHERE id = ? AND deleted = 0";
                    java.util.List<Map<String, Object>> users = 
                        jdbcTemplate.queryForList(userSql, comment.getUserId());
                    if (!users.isEmpty()) {
                        Map<String, Object> user = users.get(0);
                        commentMap.put("username", user.get("username"));
                        commentMap.put("nickname", user.get("nickname") != null ? user.get("nickname") : user.get("username"));
                    }
                } catch (Exception e) {
                    log.warn("查询用户信息失败 - userId: {}", comment.getUserId(), e);
                }
                
                return commentMap;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", commentsWithUser,
            "total", page.getTotal(),
            "pageNum", pageNum,
            "pageSize", pageSize
        ));
    }

    /**
     * 添加评论
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addComment(
            HttpServletRequest servletRequest,
            @RequestBody ProductComment comment) {
        // 从拦截器中获取真实用户ID
        Long userId = (Long) servletRequest.getAttribute("userId");
        log.info("📝 收到添加评论请求 - 商品ID: {}, 用户ID: {}", comment.getProductId(), userId);
        
        if (userId == null) {
            log.warn("❌ 添加评论失败 - 未获取到用户ID，可能未登录");
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        
        comment.setUserId(userId);
        log.info("✅ 设置评论userId为: {}", userId);
        
        ProductComment created = commentService.addComment(comment);
        log.info("✅ 评论添加成功 - 评论ID: {}, userId: {}", created.getId(), created.getUserId());
        
        return ResponseEntity.ok(Map.of("success", true, "data", created));
    }

    /**
     * AI分析评论情感
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyzeComment(@PathVariable Long id) {
        try {
            Map<String, Object> result = commentService.analyzeCommentSentiment(id);
            return ResponseEntity.ok(Map.of("success", true, "analysis", result));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除评论（仅评论作者可删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            HttpServletRequest servletRequest,
            @PathVariable Long id) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        log.info("🗑️ 收到删除评论请求 - 评论ID: {}, 用户ID: {}", id, userId);
        
        if (userId == null) {
            log.warn("❌ 删除评论失败 - 未获取到用户ID，可能未登录");
            return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
        }
        
        try {
            boolean deleted = commentService.deleteComment(id, userId);
            if (deleted) {
                log.info("✅ 删除评论成功 - 评论ID: {}, 用户ID: {}", id, userId);
                return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
            } else {
                log.warn("⚠️ 删除评论失败 - 无权限或评论不存在 - 评论ID: {}, 用户ID: {}", id, userId);
                return ResponseEntity.ok(Map.of("success", false, "message", "无权限删除或评论不存在"));
            }
        } catch (Exception e) {
            log.error("❌ 删除评论异常 - 评论ID: {}, 用户ID: {}", id, userId, e);
            return ResponseEntity.ok(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }
}
