package com.shop.clothingstore.repository;

import java.util.Optional;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface CategoryRepository extends BaseRepository<Category, Long> {

    Optional<Category> findByName(String name);

    Optional<Category> findBySlug(String slug);
}