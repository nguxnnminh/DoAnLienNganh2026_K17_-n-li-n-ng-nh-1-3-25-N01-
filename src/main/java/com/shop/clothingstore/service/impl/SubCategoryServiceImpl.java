package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.SubCategoryService;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class SubCategoryServiceImpl
        extends GenericServiceBase<SubCategory, Long>
        implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public SubCategoryServiceImpl(SubCategoryRepository repo) {
        super(repo);
        this.subCategoryRepository = repo;
    }

    // ============================
    // GIỮ NGUYÊN LOGIC CŨ
    // ============================
    @Override
    public List<SubCategory> getAllSubCategories() {
        return findAll();
    }

    @Override
    public Optional<SubCategory> getSubCategoryById(Long id) {
        return findById(id);
    }

    @Override
    public List<SubCategory> getByCategoryId(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }

    @Override
    public Optional<SubCategory> getBySlug(String slug) {
        return subCategoryRepository.findBySlug(slug);
    }
}
