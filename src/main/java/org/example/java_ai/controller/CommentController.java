package org.example.java_ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.entity.ProductComment;
import org.example.java_ai.service.ProductCommentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ProductCommentService commentService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/product/{productId}")
    public Result<Map<String, Object>> listComments(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        log.info("查询评论 - 商品ID: {}, 页码: {}", productId, pageNum);
        Page<ProductComment> page = commentService.listComments(productId, pageNum, pageSize);

        List<Long> userIds = page.getRecords().stream()
                .map(ProductComment::getUserId).distinct().toList();
        Map<Long, Map<String, Object>> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            String inClause = userIds.stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            var users = jdbcTemplate.queryForList(
                    "SELECT id, username, nickname FROM sys_user WHERE id IN (" + inClause + ") AND deleted = 0");
            for (var user : users) {
                userMap.put(((Number) user.get("id")).longValue(),
                        Map.of("username", (String) user.get("username"),
                                "nickname", user.get("nickname") != null ? (String) user.get("nickname") : (String) user.get("username")));
            }
        }

        List<Map<String, Object>> commentsWithUser = page.getRecords().stream()
            .map(comment -> {
                Map<String, Object> cm = new java.util.HashMap<>();
                cm.put("id", comment.getId());
                cm.put("productId", comment.getProductId());
                cm.put("userId", comment.getUserId());
                cm.put("content", comment.getContent());
                cm.put("rating", comment.getRating());
                cm.put("sentiment", comment.getSentiment());
                cm.put("aiTags", comment.getAiTags());
                cm.put("summary", comment.getSummary());
                cm.put("createTime", comment.getCreateTime() != null ? comment.getCreateTime().toString() : null);
                Map<String, Object> userInfo = userMap.get(comment.getUserId());
                if (userInfo != null) {
                    cm.put("username", userInfo.get("username"));
                    cm.put("nickname", userInfo.get("nickname"));
                }
                return cm;
            }).toList();

        return Result.success(Map.of(
            "data", commentsWithUser, "total", page.getTotal(),
            "pageNum", pageNum, "pageSize", pageSize));
    }

    @PostMapping
    public Result<ProductComment> addComment(HttpServletRequest servletRequest,
                                             @RequestBody ProductComment comment) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        comment.setUserId(userId);
        ProductComment created = commentService.addComment(comment);
        return Result.success(created);
    }

    @PostMapping("/{id}/analyze")
    public Result<Map<String, Object>> analyzeComment(@PathVariable Long id) {
        return Result.success(commentService.analyzeCommentSentiment(id));
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> deleteComment(HttpServletRequest servletRequest,
                                                     @PathVariable Long id) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        boolean deleted = commentService.deleteComment(id, userId);
        return deleted ? Result.success(Map.of("message", "删除成功"))
                : Result.error("无权限删除或评论不存在");
    }
}
