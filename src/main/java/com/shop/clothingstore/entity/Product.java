package com.shop.clothingstore.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.entity.base.ItemVariant;
import com.shop.clothingstore.entity.base.SellableItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString(exclude = {"productVariants", "images"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Product extends BaseEntity implements SellableItem {

    @EqualsAndHashCode.Include
    private Long id;

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

    // ================= VARIANTS =================
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<ProductVariant> productVariants = new HashSet<>();

    // ================= IMAGES =================
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<ProductImage> images = new HashSet<>();

    // ================= INTERFACE IMPLEMENT =================
    @Override
    public Double getMinPrice() {
        return productVariants.stream()
                .map(ProductVariant::getPrice)
                .min(Double::compareTo)
                .orElse(0.0);
    }

    @Override
    public Integer getTotalStock() {
        return productVariants.stream()
                .mapToInt(ProductVariant::getStock)
                .sum();
    }

    @Override
    public Integer getTotalSold() {
        return productVariants.stream()
                .mapToInt(ProductVariant::getSold)
                .sum();
    }

    @Override
    public List<? extends ItemVariant> getVariants() {
        return productVariants.stream().toList();
    }

    @Override
    public List<ProductImage> getImages() {
        return images.stream().toList();
    }

    // ================= HELPER METHODS =================
    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        productVariants.add(variant);
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }
}
