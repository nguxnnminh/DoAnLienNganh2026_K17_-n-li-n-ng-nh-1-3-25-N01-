package com.shop.clothingstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.SubCategory;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
}
