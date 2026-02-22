package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.base.GenericServiceImpl;

@Service
public class CategoryServiceImpl
        extends GenericServiceImpl<Category, Long>
        implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        super(categoryRepository); // truyền lên GenericServiceImpl
        this.categoryRepository = categoryRepository;
    }

    // ============================
    // GIỮ NGUYÊN BEHAVIOR CŨ
    // ============================

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
}