package com.shop.clothingstore.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String metaTitle;

    @Column(columnDefinition = "TEXT")
    private String metaDescription;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    private boolean active = true;

    // Denormalized for efficient sorting — updated whenever variants change.
    // Using a column so Spring Data Pageable can ORDER BY min_price.
    @Column(name = "min_price", precision = 19, scale = 2)
    private BigDecimal minPrice = BigDecimal.ZERO;

    // ================= VARIANTS =================
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ProductVariant> productVariants = new ArrayList<>();

    // ================= IMAGES =================
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<ProductImage> images = new HashSet<>();

    // ================= INTERFACE IMPLEMENTATION =================

    @Override
    public BigDecimal getMinPrice() {
        return minPrice != null ? minPrice : BigDecimal.ZERO;
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
        return new ArrayList<>(productVariants);
    }

    /**
     * Returns a mutable copy of the image collection.
     * Use removeImagesByIds() or addImage() to mutate the actual collection.
     */
    @Override
    public List<ProductImage> getImages() {
        return new ArrayList<>(images);
    }

    // ================= HELPER METHODS =================

    public void addVariant(ProductVariant variant) {
        if (!productVariants.contains(variant)) {
            variant.setProduct(this);
            productVariants.add(variant);
        }
        refreshMinPrice();
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    /**
     * Remove images whose IDs are in the deletion list.
     * Operates on the actual backing Set, bypassing the read-only copy from getImages().
     */
    public void removeImagesByIds(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        images.removeIf(img -> imageIds.contains(img.getId()));
    }

    /**
     * Recompute the denormalized minPrice from current variants.
     * Call after any variant add/update/remove.
     */
    public void refreshMinPrice() {
        this.minPrice = productVariants.stream()
                .map(ProductVariant::getPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
