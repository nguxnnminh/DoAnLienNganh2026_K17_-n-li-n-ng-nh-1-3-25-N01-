package com.shop.clothingstore.service;

import java.util.List;
import java.util.Optional;

import com.shop.clothingstore.entity.SubCategory;

public interface SubCategoryService {

    List<SubCategory> getAllSubCategories();

    Optional<SubCategory> getSubCategoryById(Long id);

    List<SubCategory> getByCategoryId(Long categoryId);
}
