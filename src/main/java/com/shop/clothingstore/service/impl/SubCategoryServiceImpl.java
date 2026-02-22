package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.SubCategoryService;
import com.shop.clothingstore.service.base.GenericServiceImpl;

@Service
public class SubCategoryServiceImpl
        extends GenericServiceImpl<SubCategory, Long>
        implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public SubCategoryServiceImpl(SubCategoryRepository subCategoryRepository) {
        super(subCategoryRepository); // truyền lên GenericServiceImpl
        this.subCategoryRepository = subCategoryRepository;
    }

    // ============================
    // GIỮ NGUYÊN LOGIC CŨ
    // ============================

    @Override
    public List<SubCategory> getAllSubCategories() {
        return subCategoryRepository.findAll();
    }

    @Override
    public Optional<SubCategory> getSubCategoryById(Long id) {
        return subCategoryRepository.findById(id);
    }

    @Override
    public List<SubCategory> getByCategoryId(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }
}