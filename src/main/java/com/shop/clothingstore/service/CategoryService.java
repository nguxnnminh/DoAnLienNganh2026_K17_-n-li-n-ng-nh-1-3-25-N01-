package com.shop.clothingstore.service;

import java.util.List;
import java.util.Optional;

import com.shop.clothingstore.entity.Category;

public interface CategoryService {

    List<Category> getAllCategories();

    Optional<Category> getCategoryById(Long id);

    Optional<Category> getCategoryBySlug(String slug);

    Category saveCategory(Category category);

    void deleteCategory(Long id);
}
