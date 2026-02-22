package com.shop.clothingstore.entity.base;

import java.util.List;

import com.shop.clothingstore.entity.ProductImage;

public interface SellableItem {

    String getName();
    String getSlug();
    String getDescription();
    boolean isActive();

    Double getMinPrice();
    Integer getTotalStock();
    Integer getTotalSold();

    List<? extends ItemVariant> getVariants();
    List<ProductImage> getImages();  // Giữ nguyên vì image chung, có thể generalize sau nếu cần

    // Không cần thêm gì nữa, vì các method này đã khớp với Product hiện tại
}