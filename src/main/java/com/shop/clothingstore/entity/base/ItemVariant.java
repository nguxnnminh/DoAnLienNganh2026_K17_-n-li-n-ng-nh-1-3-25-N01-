package com.shop.clothingstore.entity.base;

import java.math.BigDecimal;

/**
 * Interface common to all product variant types.
 * getPrice() uses BigDecimal for financial precision — never Double for money.
 */
public interface ItemVariant {

    String getIdentifier();

    BigDecimal getPrice();

    Integer getStock();

    Integer getSold();

    SellableItem getItem();
}
