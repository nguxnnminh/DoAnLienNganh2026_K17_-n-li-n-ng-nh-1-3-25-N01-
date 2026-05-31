package com.shop.clothingstore.exception;

public class ProductNotFoundException extends AppException {
    public ProductNotFoundException(String slug) {
        super("Product not found: " + slug);
    }
    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }
}
