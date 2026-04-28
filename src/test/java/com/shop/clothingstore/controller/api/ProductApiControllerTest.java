package com.shop.clothingstore.controller.api;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.security.JwtUtil;
import com.shop.clothingstore.service.CustomUserDetailsService;
import com.shop.clothingstore.service.ProductService;
import com.shop.clothingstore.service.RecommendationService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ProductApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Product mockProduct(Long id, String name) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Top");
        category.setSlug("top");

        SubCategory sub = new SubCategory();
        sub.setId(2L);
        sub.setName("Tee");
        sub.setSlug("tee");
        sub.setCategory(category);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug(name.toLowerCase().replace(" ", "-"));
        product.setDescription("Test description");
        product.setActive(true);
        product.setSubCategory(sub);
        return product;
    }

    // =============================================================
    // GET /api/products
    // =============================================================
    @Test
    @DisplayName("GET /api/products returns paginated products")
    void getProducts_Default_ReturnsOk() throws Exception {
        Product p = mockProduct(1L, "Essential Tee");
        when(productService.findWithFilter(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Essential Tee"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/products with sort param works")
    void getProducts_WithSort_ReturnsOk() throws Exception {
        Product p = mockProduct(2L, "Graphic Tee");
        when(productService.findWithFilter(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        mockMvc.perform(get("/api/products?sort=newest&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Graphic Tee"));
    }

    // =============================================================
    // GET /api/products/{id}
    // =============================================================
    @Test
    @DisplayName("GET /api/products/{id} returns product details")
    void getProductById_Exists_ReturnsOk() throws Exception {
        Product p = mockProduct(1L, "Essential Tee");
        when(productService.findById(1L)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Essential Tee"))
                .andExpect(jsonPath("$.categoryName").value("Top"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 when not found")
    void getProductById_NotFound_Returns404() throws Exception {
        when(productService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    // =============================================================
    // GET /api/products/{id}/similar
    // =============================================================
    @Test
    @DisplayName("GET /api/products/{id}/similar returns similar products")
    void getSimilarProducts_ReturnsOk() throws Exception {
        Product p = mockProduct(3L, "Similar Tee");
        when(recommendationService.getSimilarProducts(1L, 6)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products/1/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Similar Tee"));
    }
}
