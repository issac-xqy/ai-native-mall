package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.example.java_ai.config.MybatisPlusInterceptorConfig;
import org.example.java_ai.entity.Product;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(MybatisPlusInterceptorConfig.class)
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("ProductMapper 单元测试")
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    private Product createAndSave(String name, String price) {
        Product p = new Product();
        p.setName(name);
        p.setCategoryId(1L);
        p.setPrice(new BigDecimal(price));
        p.setStock(100);
        p.setSales(0);
        p.setStatus(1);
        p.setPublishStatus(1);
        productMapper.insert(p);
        return p;
    }

    // ==================== 基础 CRUD ====================

    @Test
    @DisplayName("insert+selectById-插入后能查到-字段正确")
    void insertAndSelectById_RoundTrip() {
        Product saved = createAndSave("测试手机", "2999.00");

        Product found = productMapper.selectById(saved.getId());

        assertNotNull(found);
        assertEquals("测试手机", found.getName());
        assertEquals(0, new BigDecimal("2999.00").compareTo(found.getPrice()));
        assertEquals(100, found.getStock());
    }

    @Test
    @DisplayName("selectById-不存在的ID-返回null")
    void selectById_NonExistentId_ReturnsNull() {
        assertNull(productMapper.selectById(99999L));
    }

    @Test
    @DisplayName("updateById-修改价格和库存-更新成功")
    void updateById_ChangePriceAndStock_UpdatesSuccessfully() {
        Product saved = createAndSave("改价商品", "5000.00");

        saved.setPrice(new BigDecimal("3999.00"));
        saved.setStock(50);
        productMapper.updateById(saved);

        Product updated = productMapper.selectById(saved.getId());
        assertEquals(0, new BigDecimal("3999.00").compareTo(updated.getPrice()));
        assertEquals(50, updated.getStock());
    }

    // ==================== 自定义 SQL ====================

    @Test
    @DisplayName("selectByKeyword-关键词匹配商品名称-返回匹配结果")
    void selectByKeyword_MatchingName_ReturnsResults() {
        createAndSave("华为Mate60旗舰手机", "6999.00");
        createAndSave("小米14 Pro", "4999.00");

        List<Product> results = productMapper.selectByKeyword("华为", 10);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(p -> p.getName().contains("华为")));
    }

    @Test
    @DisplayName("selectByKeyword-无匹配关键词-返回空列表")
    void selectByKeyword_NoMatch_ReturnsEmptyList() {
        List<Product> results = productMapper.selectByKeyword("不存在的关键词XYZ", 10);

        assertEquals(0, results.size());
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("selectPage-分页查询-size=2-只返回2条")
    void selectPage_PageSize2_Returns2Records() {
        createAndSave("商品A", "100.00");
        createAndSave("商品B", "200.00");
        createAndSave("商品C", "300.00");

        Page<Product> page = new Page<>(1, 2);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getPublishStatus, 1);

        Page<Product> result = productMapper.selectPage(page, wrapper);

        assertEquals(2, result.getRecords().size());
        assertTrue(result.getTotal() >= 3);
    }

    @Test
    @DisplayName("selectPage-按价格降序-第一条 >= 第二条")
    void selectPage_OrderByPriceDesc_FirstPriceGteSecond() {
        createAndSave("便宜", "100.00");
        createAndSave("贵", "99999.00");

        Page<Product> page = new Page<>(1, 10);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getPublishStatus, 1)
               .orderByDesc(Product::getPrice);

        Page<Product> result = productMapper.selectPage(page, wrapper);
        List<Product> records = result.getRecords();

        assertTrue(records.size() >= 2);
        assertTrue(records.get(0).getPrice().compareTo(records.get(1).getPrice()) >= 0);
    }

    // ==================== 逻辑删除 ====================

    @Test
    @DisplayName("deleteById-逻辑删除-set deleted=1 后selectById返回null")
    void deleteById_LogicDelete_SelectReturnsNull() {
        Product saved = createAndSave("待删除", "99.00");

        int rows = productMapper.deleteById(saved.getId());
        assertEquals(1, rows);

        assertNull(productMapper.selectById(saved.getId()));
    }

    // ==================== 批量操作 ====================

    @Test
    @DisplayName("批量插入5条-selectCount增加5")
    void insert_Batch_CountIncreasesBy5() {
        long before = productMapper.selectCount(null);

        for (int i = 1; i <= 5; i++) {
            createAndSave("批量商品" + i, "100.00");
        }

        assertEquals(before + 5, productMapper.selectCount(null));
    }
}
