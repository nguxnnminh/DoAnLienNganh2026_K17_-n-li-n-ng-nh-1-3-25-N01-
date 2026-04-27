package com.shop.clothingstore.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.CategoryResponse;
import com.shop.clothingstore.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET /api/categories
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {

        List<CategoryResponse> categories = categoryService.getAllCategories().stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(categories);
    }
}
