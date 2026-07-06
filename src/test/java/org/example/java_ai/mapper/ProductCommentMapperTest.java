package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.example.java_ai.entity.ProductComment;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("ProductCommentMapper 单元测试")
class ProductCommentMapperTest {

    @Autowired
    private ProductCommentMapper commentMapper;

    @Test
    @DisplayName("insert+selectByProductId-插入评论后按商品查询")
    void insertAndSelectByProductId() {
        ProductComment c = new ProductComment();
        c.setProductId(1L);
        c.setUserId(1L);
        c.setContent("很好用！");
        c.setSentiment("positive");
        c.setRating(5);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        commentMapper.insert(c);
        assertNotNull(c.getId());

        List<ProductComment> comments = commentMapper.selectByProductId(1L, 0, 10);
        assertFalse(comments.isEmpty());
        assertTrue(comments.stream().anyMatch(cm -> "很好用！".equals(cm.getContent())));
    }

    @Test
    @DisplayName("countByProductId-统计商品评论数")
    void countByProductId() {
        ProductComment c = new ProductComment();
        c.setProductId(2L);
        c.setUserId(1L);
        c.setContent("不错");
        c.setRating(4);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        commentMapper.insert(c);

        Long count = commentMapper.countByProductId(2L);
        assertTrue(count >= 1);
    }

    @Test
    @DisplayName("deleteById-逻辑删除-评论查不到")
    void logicDeleteComment() {
        ProductComment c = new ProductComment();
        c.setProductId(3L);
        c.setUserId(1L);
        c.setContent("要删的评论");
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        commentMapper.insert(c);

        commentMapper.deleteById(c.getId());

        List<ProductComment> comments = commentMapper.selectByProductId(3L, 0, 10);
        assertTrue(comments.stream().noneMatch(cm -> cm.getId().equals(c.getId())));
    }

    @Test
    @DisplayName("selectByProductId-无评论商品-返回空列表")
    void selectByProductId_NoComments() {
        List<ProductComment> comments = commentMapper.selectByProductId(9999L, 0, 10);
        assertTrue(comments.isEmpty());
    }
}
