package com.shop.clothingstore.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.dto.api.ProductResponse;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.exception.ResourceNotFoundException;
import com.shop.clothingstore.service.ProductService;
import com.shop.clothingstore.service.RecommendationService;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final RecommendationService recommendationService;

    public ProductApiController(ProductService productService,
            RecommendationService recommendationService) {
        this.productService = productService;
        this.recommendationService = recommendationService;
    }

    // =====================================================
    // GET /api/products?page=0&size=12&sort=newest&keyword=...
    // =====================================================
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setKeyword(keyword);
        filter.setCategoryId(categoryId);
        filter.setSubCategoryId(subCategoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);

        Pageable pageable = buildPageable(page, size, sort);

        Page<Product> products = productService.findWithFilter(filter, pageable);

        List<ProductResponse> content = products.getContent().stream()
                .map(ProductResponse::summary)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", products.getNumber());
        response.put("size", products.getSize());
        response.put("totalElements", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // GET /api/products/{id}
    // =====================================================
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {

        Product product = productService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    // =====================================================
    // GET /api/products/{id}/similar?limit=6
    // =====================================================
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ProductResponse>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limit) {

        List<ProductResponse> result = recommendationService
                .getSimilarProducts(id, limit).stream()
                .map(ProductResponse::summary)
                .toList();

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // HELPER
    // =====================================================
    private Pageable buildPageable(int page, int size, String sort) {

        Sort sortObj;

        if (sort == null) {
            sortObj = Sort.by("id").descending();
        } else {
            switch (sort) {
                case "oldest":
                    sortObj = Sort.by("createdAt").ascending();
                    break;
                case "name_asc":
                    sortObj = Sort.by("name").ascending();
                    break;
                case "name_desc":
                    sortObj = Sort.by("name").descending();
                    break;
                case "newest":
                default:
                    sortObj = Sort.by("id").descending();
                    break;
            }
        }

        return PageRequest.of(page, Math.min(size, 50), sortObj);
    }
}
