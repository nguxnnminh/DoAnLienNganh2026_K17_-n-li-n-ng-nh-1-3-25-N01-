package com.shop.clothingstore.entity;

import java.math.BigDecimal;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

    // Snapshot data — price at time of purchase
    private String productName;
    private String size;
    private String color;

    // FIXED: BigDecimal for monetary precision
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    private Integer quantity;

    // Track which variant this item refers to (for stock restore on cancel)
    private Long variantId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Computed helper — not persisted
    @Transient
    public BigDecimal getLineTotal() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
