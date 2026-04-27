package com.shop.clothingstore.exception;

public class OutOfStockException extends RuntimeException {

    private final String productName;
    private final int availableStock;

    public OutOfStockException(String productName, int availableStock) {
        super("Không đủ tồn kho cho sản phẩm: " + productName
                + ". Còn lại: " + availableStock);
        this.productName = productName;
        this.availableStock = availableStock;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableStock() {
        return availableStock;
    }
}
