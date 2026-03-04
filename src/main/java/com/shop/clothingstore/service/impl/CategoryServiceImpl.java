package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class CategoryServiceImpl
        extends GenericServiceBase<Category, Long>
        implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        super(categoryRepository);
        this.categoryRepository = categoryRepository;
    }

    // ============================
    // GIỮ NGUYÊN BEHAVIOR CŨ
    // ============================
    @Override
    public List<Category> getAllCategories() {
        return findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return findById(id);
    }

    @Override
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }
}
