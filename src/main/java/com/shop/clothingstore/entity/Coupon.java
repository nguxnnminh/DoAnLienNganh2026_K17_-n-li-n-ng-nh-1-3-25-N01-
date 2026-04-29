package com.shop.clothingstore.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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

    // BigDecimal for all monetary/percentage values — never Double for finance
    @NotNull
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @DecimalMin(value = "0", inclusive = true)
    @Column(precision = 19, scale = 2)
    private BigDecimal minOrderAmount;

    private LocalDateTime expiryDate;

    private Integer usageLimit;

    private Integer usageCount = 0;

    private boolean active = true;

    public enum DiscountType {
        PERCENTAGE,
        FIXED
    }

    public boolean isValid(BigDecimal orderTotal) {
        if (!active) {
            return false;
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            return false;
        }
        if (usageLimit != null && usageCount >= usageLimit) {
            return false;
        }
        return minOrderAmount == null || orderTotal.compareTo(minOrderAmount) >= 0;
    }

    /**
     * Apply discount and return the final order total.
     * PERCENTAGE: discountValue is 0–100 (e.g., 20 = 20% off).
     * FIXED: discountValue is the flat amount to subtract.
     * Result is always ≥ 0, rounded to 0 decimal places (Vietnamese VND).
     */
    public BigDecimal applyDiscount(BigDecimal orderTotal) {
        if (discountType == DiscountType.PERCENTAGE) {
            // Guard: percentage must be 0 < value ≤ 100
            BigDecimal pct = discountValue.min(BigDecimal.valueOf(100));
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    pct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            return orderTotal.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
        } else {
            return orderTotal.subtract(discountValue).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
        }
    }
}
