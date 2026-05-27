package org.example.java_ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.java_ai.entity.Product;
import org.example.java_ai.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController MockMvc 测试")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    // ==================== GET /api/product/list ====================

    @Test
    @DisplayName("商品列表-默认参数-返回分页数据")
    void listProducts_DefaultParams_ReturnsPage() throws Exception {
        Page<Product> page = buildPage(3);
        when(productService.listProducts(eq(1), eq(10), isNull(), isNull(), eq("create_time"), eq("desc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @DisplayName("商品列表-自定义分页参数-正确传递")
    void listProducts_CustomParams_PassesCorrectly() throws Exception {
        when(productService.listProducts(eq(1), eq(5), isNull(), isNull(), eq("create_time"), eq("desc")))
                .thenReturn(buildPage(0));

        mockMvc.perform(get("/api/product/list")
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("商品列表-分类+关键词+排序-全部传递")
    void listProducts_AllFilters_PassesAll() throws Exception {
        when(productService.listProducts(eq(1), eq(10), eq(1L), eq("手机"), eq("price"), eq("asc")))
                .thenReturn(buildPage(2));

        mockMvc.perform(get("/api/product/list")
                        .param("categoryId", "1")
                        .param("keyword", "手机")
                        .param("sortField", "price")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    // ==================== GET /api/product/{id} ====================

    @Test
    @DisplayName("商品详情-商品存在-返回商品数据")
    void getProduct_Exists_ReturnsProduct() throws Exception {
        Product product = buildProduct(1L, "iPhone 15", "5999.00", 50);
        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("iPhone 15"))
                .andExpect(jsonPath("$.data.price").value(5999))
                .andExpect(jsonPath("$.data.stock").value(50));
    }

    @Test
    @DisplayName("商品详情-商品不存在-返回错误")
    void getProduct_NotExists_ReturnsError() throws Exception {
        when(productService.getProductById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/product/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }

    // ==================== POST /api/product ====================

    @Test
    @DisplayName("创建商品-正常数据-返回创建的商品")
    void createProduct_ValidData_ReturnsCreated() throws Exception {
        Product input = buildProduct(null, "新商品", "1999.00", 100);
        when(productService.createProduct(any(Product.class))).thenReturn(input);

        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"新商品","price":1999.00,"stock":100,"categoryId":1,"status":1}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("新商品"));
    }

    // ==================== PUT /api/product/{id} ====================

    @Test
    @DisplayName("更新商品-正常数据-返回更新结果")
    void updateProduct_ValidData_ReturnsUpdated() throws Exception {
        Product updated = buildProduct(1L, "改后商品", "2599.00", 80);
        when(productService.updateProduct(any(Product.class))).thenReturn(updated);

        mockMvc.perform(put("/api/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"改后商品","price":2599.00,"stock":80}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("改后商品"));
    }

    // ==================== DELETE /api/product/{id} ====================

    @Test
    @DisplayName("删除商品-正常删除-返回成功")
    void deleteProduct_Normal_ReturnsSuccess() throws Exception {
        when(productService.deleteProduct(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除商品-商品不存在-返回错误")
    void deleteProduct_NotExists_ReturnsError() throws Exception {
        when(productService.deleteProduct(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/product/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }

    // ==================== GET /api/product/top-sales ====================

    @Test
    @DisplayName("销量排行-默认limit-返回Top10")
    void getTopSalesProducts_Default_ReturnsTop10() throws Exception {
        when(productService.getTopSalesProducts(10))
                .thenReturn(List.of(buildProduct(1L, "爆款", "99.00", 100)));

        mockMvc.perform(get("/api/product/top-sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("爆款"));
    }

    // ==================== GET /api/product/top-rated ====================

    @Test
    @DisplayName("好评排行-默认limit-返回Top10")
    void getTopRatedProducts_Default_ReturnsTop10() throws Exception {
        when(productService.getTopRatedProducts(10))
                .thenReturn(List.of(buildProduct(1L, "好评商品", "199.00", 50)));

        mockMvc.perform(get("/api/product/top-rated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("好评商品"));
    }

    // ==================== helpers ====================

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
