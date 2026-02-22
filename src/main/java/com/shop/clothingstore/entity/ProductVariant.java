package com.shop.clothingstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.entity.base.ItemVariant;     // Thêm import này
import com.shop.clothingstore.entity.base.SellableItem;   // Thêm import này

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "product_variants")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariant extends BaseEntity implements ItemVariant {

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer sold = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    // Các method override để khớp interface ItemVariant
    @Override
    public String getIdentifier() {
        // Tổng quát hóa size + color thành một chuỗi duy nhất (có thể thay đổi format sau này)
        return (size != null ? size : "") + "-" + (color != null ? color : "");
    }

    @Override
    public Double getPrice() {
        return price;
    }

    @Override
    public Integer getStock() {
        return stock;
    }

    @Override
    public Integer getSold() {
        return sold;
    }

    @Override
    public SellableItem getItem() {
        return product;  // Product implements SellableItem
    }
}