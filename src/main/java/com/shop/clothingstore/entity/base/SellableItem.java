package com.shop.clothingstore.entity.base;

import java.math.BigDecimal;
import java.util.List;

public interface SellableItem {

    String getName();

    String getSlug();

    String getDescription();

    boolean isActive();

    BigDecimal getMinPrice();

    Integer getTotalStock();

    Integer getTotalSold();

    List<? extends ItemVariant> getVariants();

    List<? extends ItemImage> getImages();
}
