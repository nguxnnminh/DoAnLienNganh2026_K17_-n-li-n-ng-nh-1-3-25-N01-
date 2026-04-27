package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class CategoryServiceImpl
        extends GenericServiceBase<Category, Long>
        implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        super(categoryRepository);
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        log.debug("Loading categories from DB (cache miss)");
        return findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return findById(id);
    }

    @Override
    // Removed buggy unless expression - Optional wrapping causes SpelEvaluationException
    // when Spring Cache tries to unwrap the Optional and calls isPresent() on Category
    public Optional<Category> getCategoryBySlug(String slug) {
        log.debug("Loading category by slug from DB: {}", slug);
        return categoryRepository.findBySlug(slug);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category saveCategory(Category category) {
        log.info("Saving category, evicting cache | name={}", category.getName());
        return save(category);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category, evicting cache | id={}", id);
        delete(id);
    }
}
