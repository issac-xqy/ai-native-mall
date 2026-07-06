package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.example.java_ai.entity.ProductCategory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("ProductCategoryMapper 单元测试")
class ProductCategoryMapperTest {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Test
    @DisplayName("insert+selectById-插入分类后能查到")
    void insertAndSelectById() {
        ProductCategory c = new ProductCategory();
        c.setName("手机数码");
        c.setParentId(0L);
        c.setSortOrder(1);
        c.setStatus(1);
        categoryMapper.insert(c);

        ProductCategory found = categoryMapper.selectById(c.getId());
        assertNotNull(found);
        assertEquals("手机数码", found.getName());
    }

    @Test
    @DisplayName("selectCount-统计分类数量")
    void selectCount() {
        long before = categoryMapper.selectCount(null);
        ProductCategory c = new ProductCategory();
        c.setName("测试分类");
        c.setParentId(0L);
        c.setStatus(1);
        categoryMapper.insert(c);
        assertEquals(before + 1, categoryMapper.selectCount(null));
    }

    @Test
    @DisplayName("逻辑删除-删后查不到")
    void logicDelete() {
        ProductCategory c = new ProductCategory();
        c.setName("待删除分类");
        c.setStatus(1);
        categoryMapper.insert(c);

        categoryMapper.deleteById(c.getId());
        assertNull(categoryMapper.selectById(c.getId()));
    }
}
