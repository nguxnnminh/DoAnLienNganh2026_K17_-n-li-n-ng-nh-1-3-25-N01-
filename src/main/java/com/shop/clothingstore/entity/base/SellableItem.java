package com.shop.clothingstore.entity.base;

import java.util.List;

public interface SellableItem {

    String getName();

    String getSlug();

    String getDescription();

    boolean isActive();

    Double getMinPrice();

    Integer getTotalStock();

    Integer getTotalSold();

    List<? extends ItemVariant> getVariants();

    List<? extends ItemImage> getImages();

    // Không cần thêm gì nữa, vì các method này đã khớp với Product hiện tại
}
