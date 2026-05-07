package com.shop.clothingstore.entity;

/**
 * Classification of garments for virtual try-on.
 * The AI model needs to know which body region to target.
 */
public enum GarmentType {
    UPPER_BODY,   // T-shirts, jackets, hoodies, blouses
    LOWER_BODY    // Pants, shorts, skirts
}
