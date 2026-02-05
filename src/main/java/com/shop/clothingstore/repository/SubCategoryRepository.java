package com.shop.clothingstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.SubCategory;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    // Thêm method này để lấy tất cả sub theo category
    List<SubCategory> findByCategoryId(Long categoryId);
}