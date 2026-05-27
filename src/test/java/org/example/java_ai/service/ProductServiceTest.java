package org.example.java_ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.java_ai.entity.Product;
import org.example.java_ai.mapper.ProductCommentMapper;
import org.example.java_ai.mapper.ProductMapper;
import org.example.java_ai.service.ai.ProductOperationService;
import org.example.java_ai.service.ai.ProductSemanticSearchService;
import org.example.java_ai.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductService 单元测试")
class ProductServiceTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductOperationService productOperationService;
    @Mock private ProductCommentMapper productCommentMapper;
    @Mock private ProductSemanticSearchService semanticSearchService;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);
    }

    @Test
    @DisplayName("listProducts-默认参数-返回分页数据")
    void listProducts_DefaultParams_ReturnsPage() {
        doReturn(buildPage(3)).when(productMapper).selectPage(any(Page.class), any());

        Page<Product> result = productService.listProducts(1, 10, null, null, "create_time", "desc");

        assertEquals(3, result.getTotal());
    }

    @Test
    @DisplayName("listProducts-分类筛选-正常返回")
    void listProducts_WithCategory_Filters() {
        doReturn(buildPage(1)).when(productMapper).selectPage(any(Page.class), any());

        Page<Product> result = productService.listProducts(1, 10, 1L, null, "create_time", "desc");

        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("listProducts-关键词搜索-正常返回")
    void listProducts_WithKeyword_Searches() {
        doReturn(buildPage(2)).when(productMapper).selectPage(any(Page.class), any());

        Page<Product> result = productService.listProducts(1, 10, null, "iPhone", "create_time", "desc");

        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("getProductById-商品存在-返回商品")
    void getProductById_Exists_ReturnsProduct() {
        doReturn(buildProduct(1L, "iPhone 15", "5999.00", 50))
                .when(productMapper).selectOne(any(), anyBoolean());

        Product result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals("iPhone 15", result.getName());
    }

    @Test
    @DisplayName("getProductById-商品不存在-返回null")
    void getProductById_NotExists_ReturnsNull() {
        doReturn(null).when(productMapper).selectOne(any(), anyBoolean());

        assertNull(productService.getProductById(999L));
    }

    @Test
    @DisplayName("createProduct-正常创建-调用save+索引")
    void createProduct_Normal_SavesAndIndexes() {
        Product product = buildProduct(null, "新商品", "1999.00", 100);
        doReturn(1).when(productMapper).insert((Product) any());

        Product result = productService.createProduct(product);

        assertEquals("新商品", result.getName());
        verify(semanticSearchService).indexProduct(any());
    }

    @Test
    @DisplayName("updateProduct-正常更新-调用updateById+索引")
    void updateProduct_Normal_UpdatesAndIndexes() {
        Product product = buildProduct(1L, "改后商品", "2999.00", 80);
        doReturn(1).when(productMapper).updateById((Product) any());

        assertNotNull(productService.updateProduct(product));
        verify(semanticSearchService).indexProduct(any());
    }

    @Test
    @DisplayName("deleteProduct-正常删除-返回true")
    void deleteProduct_Normal_ReturnsTrue() {
        doReturn(1).when(productMapper).deleteById(anyLong());

        assertTrue(productService.deleteProduct(1L));
        verify(semanticSearchService).removeProductIndex(1L);
    }

    @Test
    @DisplayName("getProductsByIds-正常ID列表-返回商品")
    void getProductsByIds_ValidIds_ReturnsProducts() {
        doReturn(List.of(
                buildProduct(1L, "商品A", "100.00", 10),
                buildProduct(2L, "商品B", "200.00", 20)))
                .when(productMapper).selectList(any());

        List<Product> result = productService.getProductsByIds(List.of(1L, 2L));

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getProductsByIds-空列表返回空")
    void getProductsByIds_EmptyList_ReturnsEmpty() {
        assertTrue(productService.getProductsByIds(List.of()).isEmpty());
        assertTrue(productService.getProductsByIds(null).isEmpty());
    }

    @Test
    @DisplayName("getTopSalesProducts-返回销量Top N")
    void getTopSalesProducts_Normal_ReturnsTopN() {
        doReturn(List.of(buildProduct(1L, "爆款", "199.00", 100)))
                .when(productMapper).selectList(any());

        List<Product> result = productService.getTopSalesProducts(5);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("incrementViewCount-更新成功-返回true")
    void incrementViewCount_Normal_ReturnsTrue() {
        doReturn(1).when(productMapper).update(any(), any());

        assertTrue(productService.incrementViewCount(1L));
    }

    @Test
    @DisplayName("getTopRatedProducts-返回好评排行")
    void getTopRatedProducts_Normal_ReturnsTopRated() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1L);
        row.put("name", "好评商品");
        row.put("price", new BigDecimal("99.00"));
        row.put("original_price", null);
        row.put("image", "/img/1.jpg");
        row.put("avg_rating", 4.8);
        rows.add(row);
        doReturn(rows).when(jdbcTemplate).queryForList(anyString(), eq(3));

        List<Product> result = productService.getTopRatedProducts(3);

        assertEquals(1, result.size());
        assertEquals("好评商品", result.get(0).getName());
    }

    private Product buildProduct(Long id, String name, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setPublishStatus(1);
        p.setStatus(1);
        return p;
    }

    private Page<Product> buildPage(int total) {
        Page<Product> page = new Page<>(1, 10);
        page.setTotal(total);
        List<Product> records = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            records.add(buildProduct((long) (i + 1), "商品" + (i + 1), (i + 1) * 100 + ".00", 50));
        }
        page.setRecords(records);
        return page;
    }
}
