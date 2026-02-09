package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shop.clothingstore.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    /*
     * =========================
     * PRODUCT DETAIL BY ID
     * =========================
     *
     * ⚠ KHÔNG FETCH images cùng lúc
     * → tránh MultipleBagFetchException
     */

    @Override
    @EntityGraph(attributePaths = {
            "productVariants",
            "subCategory",
            "subCategory.category"
    })
    Optional<Product> findById(Long id);


    /*
     * =========================
     * PRODUCT DETAIL BY SLUG
     * =========================
     */

    @EntityGraph(attributePaths = {
            "productVariants",
            "subCategory",
            "subCategory.category"
    })
    Optional<Product> findBySlug(String slug);


    /*
     * =========================
     * SIMPLE SEARCH
     * =========================
     */

    Optional<Product> findByName(String name);
}
