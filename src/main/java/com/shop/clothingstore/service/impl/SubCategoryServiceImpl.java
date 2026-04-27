package com.shop.clothingstore.service.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.SubCategoryService;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class SubCategoryServiceImpl
        extends GenericServiceBase<SubCategory, Long>
        implements SubCategoryService {

    private static final Logger log = LoggerFactory.getLogger(SubCategoryServiceImpl.class);

    private final SubCategoryRepository subCategoryRepository;

    public SubCategoryServiceImpl(SubCategoryRepository repo) {
        super(repo);
        this.subCategoryRepository = repo;
    }

    @Override
    @Cacheable(value = "subCategories")
    public List<SubCategory> getAllSubCategories() {
        log.debug("Loading subCategories from DB (cache miss)");
        return findAll();
    }

    @Override
    public Optional<SubCategory> getSubCategoryById(Long id) {
        return findById(id);
    }

    @Override
    @Cacheable(value = "subCategories", key = "#categoryId")
    public List<SubCategory> getByCategoryId(Long categoryId) {
        log.debug("Loading subCategories by categoryId from DB: {}", categoryId);
        return subCategoryRepository.findByCategoryId(categoryId);
    }

    @Override
    // Removed buggy unless expression - Optional wrapping causes SpelEvaluationException
    // when Spring Cache tries to unwrap the Optional and calls isPresent() on SubCategory
    public Optional<SubCategory> getBySlug(String slug) {
        log.debug("Loading subCategory by slug from DB: {}", slug);
        return subCategoryRepository.findBySlug(slug);
    }

    @Override
    @CacheEvict(value = "subCategories", allEntries = true)
    public SubCategory saveSubCategory(SubCategory subCategory) {
        log.info("Saving subCategory, evicting cache | name={}", subCategory.getName());
        return save(subCategory);
    }

    @Override
    @CacheEvict(value = "subCategories", allEntries = true)
    public void deleteSubCategory(Long id) {
        log.info("Deleting subCategory, evicting cache | id={}", id);
        delete(id);
    }
}
