package com.shop.clothingstore.repository;

import java.util.List;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface ProductVariantRepository extends BaseRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct(Product product);

}