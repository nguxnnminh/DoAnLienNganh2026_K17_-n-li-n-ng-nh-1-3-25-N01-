package com.shop.clothingstore.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.SubCategoryRepository;

@Service
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public SubCategoryService(SubCategoryRepository subCategoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
    }

    public List<SubCategory> getAllSubCategories() {
        return subCategoryRepository.findAll();
    }

    // Thêm method này
    public List<SubCategory> getSubCategoriesByCategoryId(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }
}