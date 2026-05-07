package com.shop.clothingstore.exception;

public class OutOfStockException extends AppException {
    public OutOfStockException(String productName, String size, String color) {
        super(String.format(
            "Sản phẩm '%s' (%s / %s) không đủ số lượng trong kho. Vui lòng giảm số lượng.",
            productName, size, color));
    }
}
