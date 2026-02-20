package com.shop.clothingstore.entity;

import java.util.ArrayList;
import java.util.List;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    private boolean active = true;

    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ProductVariant> productVariants = new ArrayList<>();

    @OneToMany(mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    public Double getMinPrice() {
        return productVariants.stream()
                .map(ProductVariant::getPrice)
                .min(Double::compareTo)
                .orElse(0.0);
    }

    public Integer getTotalStock() {
        return productVariants.stream()
                .mapToInt(ProductVariant::getStock)
                .sum();
    }

    public Integer getTotalSold() {
        return productVariants.stream()
                .mapToInt(ProductVariant::getSold)
                .sum();
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        productVariants.add(variant);
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }
}