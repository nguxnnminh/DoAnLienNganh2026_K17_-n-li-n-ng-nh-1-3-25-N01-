package com.shop.clothingstore.entity.base;

public interface ItemVariant {

    String getIdentifier();  // Tổng quát hóa: có thể là "S-Red" cho quần áo, hoặc "Session1" cho khóa học
    Double getPrice();
    Integer getStock();
    Integer getSold();
    SellableItem getItem();  // Quan hệ ngược về Item chung
}