package org.example.java_ai.controller.admin;

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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductController.class)
@DisplayName("AdminProductController MockMvc 测试")
class AdminProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;

    @Test
    @DisplayName("商品列表-分页查询-返回数据")
    void listProducts_Paginated_ReturnsData() throws Exception {
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(List.of(buildProduct(1L, "商品1", "100.00")));
        page.setTotal(1);
        when(productService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("商品列表-按发布状态筛选")
    void listProducts_FilterByStatus() throws Exception {
        Page<Product> page = new Page<>(1, 10);
        page.setTotal(0);
        when(productService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/product/list")
                        .param("publishStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("创建商品-调用save-成功")
    void createProduct_Success() throws Exception {
        doAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L); // 模拟 MyBatis Plus auto-fill
            return true;
        }).when(productService).save(any(Product.class));

        mockMvc.perform(post("/api/admin/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"新商品","price":1999.00,"stock":100,"categoryId":1}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("更新商品-调用updateById-成功")
    void updateProduct_Success() throws Exception {
        when(productService.updateById(any(Product.class))).thenReturn(true);

        mockMvc.perform(put("/api/admin/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"改后商品","price":2599.00}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("删除商品-调用removeById-成功")
    void deleteProduct_Success() throws Exception {
        when(productService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Product buildProduct(Long id, String name, String price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(100);
        p.setPublishStatus(1);
        return p;
    }
}
