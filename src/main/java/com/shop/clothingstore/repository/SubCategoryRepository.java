package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface SubCategoryRepository extends BaseRepository<SubCategory, Long> {

    // Lấy tất cả sub theo category
    List<SubCategory> findByCategoryId(Long categoryId);

    // Tìm subcategory bằng slug
    Optional<SubCategory> findBySlug(String slug);
}