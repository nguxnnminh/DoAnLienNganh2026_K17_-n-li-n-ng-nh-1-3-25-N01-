package com.shop.clothingstore.repository;

import java.util.List;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface ProductImageRepository extends BaseRepository<ProductImage, Long> {

    List<ProductImage> findByProduct(Product product);

}