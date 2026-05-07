package com.shop.clothingstore.exception;

public class ProductNotFoundException extends AppException {
    public ProductNotFoundException(String slug) {
        super("Không tìm thấy sản phẩm: " + slug);
    }
    public ProductNotFoundException(Long id) {
        super("Không tìm thấy sản phẩm với id: " + id);
    }
}
