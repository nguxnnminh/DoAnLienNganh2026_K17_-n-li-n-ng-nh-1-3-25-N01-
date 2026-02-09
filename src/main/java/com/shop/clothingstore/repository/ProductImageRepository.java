package com.shop.clothingstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct(Product product);

}
