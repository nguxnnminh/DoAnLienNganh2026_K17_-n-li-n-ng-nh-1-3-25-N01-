package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

        @Query("""
        SELECT DISTINCT p
        FROM Product p
        JOIN p.subCategory sc
        JOIN sc.category c
        JOIN p.productVariants pv
        WHERE c.slug = :slug
        ORDER BY pv.sold DESC
        """)
        Page<Product> findBestSellerByCategorySlug(
                @Param("slug") String slug,
                Pageable pageable
        );

}
