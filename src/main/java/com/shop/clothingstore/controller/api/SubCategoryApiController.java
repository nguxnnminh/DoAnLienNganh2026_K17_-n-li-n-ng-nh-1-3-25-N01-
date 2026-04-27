package com.shop.clothingstore.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.SubCategoryResponse;
import com.shop.clothingstore.service.SubCategoryService;

@RestController
@RequestMapping("/api/subcategories")
public class SubCategoryApiController {

    private final SubCategoryService subCategoryService;

    public SubCategoryApiController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    // GET /api/subcategories/by-category/{categoryId}
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<SubCategoryResponse>> getByCategory(
            @PathVariable Long categoryId) {

        List<SubCategoryResponse> result = subCategoryService
                .getByCategoryId(categoryId).stream()
                .map(SubCategoryResponse::from)
                .toList();

        return ResponseEntity.ok(result);
    }
}
