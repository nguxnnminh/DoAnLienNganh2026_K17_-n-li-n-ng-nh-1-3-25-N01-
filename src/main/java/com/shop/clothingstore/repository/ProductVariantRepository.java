package com.shop.clothingstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct(Product product);

}
