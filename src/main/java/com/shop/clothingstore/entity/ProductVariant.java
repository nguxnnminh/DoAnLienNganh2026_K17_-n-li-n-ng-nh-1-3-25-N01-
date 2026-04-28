package com.shop.clothingstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.entity.base.ItemVariant;
import com.shop.clothingstore.entity.base.SellableItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@ToString(exclude = "product")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProductVariant extends BaseEntity implements ItemVariant {

    @Column(unique = true)
    private String sku;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer sold = 0;

    private Double weight; // in grams, for shipping calculation

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    @Override
    public String getIdentifier() {
        return (size != null ? size : "") + "-" + (color != null ? color : "");
    }

    @Override
    public Double getPrice() {
        return price != null ? price : 0.0;
    }

    @Override
    public Integer getStock() {
        return stock != null ? stock : 0;
    }

    @Override
    public Integer getSold() {
        return sold != null ? sold : 0;
    }

    @Override
    public SellableItem getItem() {
        return product;
    }
}
