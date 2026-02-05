package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shop.clothingstore.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {
            "productVariants",
            "images",
            "subCategory",
            "subCategory.category"
    })
    Optional<Product> findById(Long id);
}
