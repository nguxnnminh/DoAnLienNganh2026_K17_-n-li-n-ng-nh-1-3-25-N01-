package com.shop.clothingstore.exception;

public class OutOfStockException extends AppException {
    public OutOfStockException(String productName, String size, String color) {
        super(String.format(
            "Product '%s' (%s / %s) is out of stock. Please reduce the quantity.",
            productName, size, color));
    }
}
