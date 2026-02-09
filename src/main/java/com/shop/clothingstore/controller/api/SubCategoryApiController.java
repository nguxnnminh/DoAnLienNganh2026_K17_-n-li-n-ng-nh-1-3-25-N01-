package com.shop.clothingstore.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.service.SubCategoryService;

@RestController
@RequestMapping("/api/subcategories")
public class SubCategoryApiController {

    private final SubCategoryService subCategoryService;

    public SubCategoryApiController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    @GetMapping("/by-category/{categoryId}")
    public List<SubCategory> getByCategory(@PathVariable Long categoryId) {
        return subCategoryService.getByCategoryId(categoryId);
    }
}
