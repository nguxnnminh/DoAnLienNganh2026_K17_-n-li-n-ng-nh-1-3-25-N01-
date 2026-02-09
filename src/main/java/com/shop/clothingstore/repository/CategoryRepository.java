package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    // Thêm để tìm category bằng slug
    Optional<Category> findBySlug(String slug);
}
