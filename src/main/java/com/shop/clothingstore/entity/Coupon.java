package com.shop.clothingstore.entity;

import java.time.LocalDateTime;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    private Double minOrderAmount;

    private LocalDateTime expiryDate;

    private Integer usageLimit;

    private Integer usageCount = 0;

    private boolean active = true;

    public enum DiscountType {
        PERCENTAGE,
        FIXED
    }

    public boolean isValid(Double orderTotal) {
        if (!active) {
            return false;
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            return false;
        }
        if (usageLimit != null && usageCount >= usageLimit) {
            return false;
        }
        return minOrderAmount == null || orderTotal >= minOrderAmount;
    }

    public Double applyDiscount(Double orderTotal) {
        if (discountType == DiscountType.PERCENTAGE) {
            return orderTotal * (1 - discountValue / 100);
        } else {
            return Math.max(0, orderTotal - discountValue);
        }
    }
}
