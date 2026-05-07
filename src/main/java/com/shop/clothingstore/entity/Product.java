package com.shop.clothingstore.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.entity.base.ItemVariant;
import com.shop.clothingstore.entity.base.SellableItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

    // ================= VIRTUAL TRY-ON =================
    @Column(name = "try_on_enabled")
    private boolean tryOnEnabled = false;

    @Column(name = "garment_processed_url")
    private String garmentProcessedUrl;

    @Column(name = "garment_type")
    @Enumerated(EnumType.STRING)
    private GarmentType garmentType;

    // Denormalized for efficient sorting — updated whenever variants change.
    // Using a column so Spring Data Pageable can ORDER BY min_price.
    @Column(name = "min_price", precision = 19, scale = 2)
    private BigDecimal minPrice = BigDecimal.ZERO;

    // Denormalized total units sold across all variants.
    // Updated on checkout completion. Enables DB-level ORDER BY total_sold DESC.
    @Column(name = "total_sold")
    private Integer totalSold = 0;

    // ================= VARIANTS =================
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ProductVariant> productVariants = new ArrayList<>();

    // ================= IMAGES =================
    // @OrderBy tells Hibernate to emit ORDER BY primary_image DESC, id ASC
    // in the secondary SELECT for this collection.
    // Must be Set (not List/bag) because productVariants is already a List;
    // Hibernate forbids JOIN-fetching two bag collections simultaneously
    // (MultipleBagFetchException). One List + one Set is always safe.
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("primaryImage DESC, id ASC")
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
     * Returns images sorted: primary image first, then by id ascending.
     * @OrderBy handles the DB-side ordering; this sort covers in-memory cases
     * (e.g. after addImage() before flush).
     */
    @Override
    public List<ProductImage> getImages() {
        return images.stream()
                .sorted(Comparator
                        .comparing(ProductImage::isPrimary, Comparator.reverseOrder())
                        .thenComparingLong(img -> img.getId() != null ? img.getId() : Long.MAX_VALUE))
                .collect(java.util.stream.Collectors.toList());
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
        if (!images.contains(image)) {
            images.add(image);
        }
    }

    /**
     * Remove images whose IDs are in the deletion list.
     * Operates on the actual backing Set, bypassing the read-only copy from getImages().
     */
    public void removeImagesByIds(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        // Operate on the actual backing List (not the defensive copy from getImages())
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

    /**
     * Recompute the denormalized totalSold from current variants.
     * Call after any variant sold count change (checkout, cancellation).
     */
    public void refreshTotalSold() {
        this.totalSold = productVariants.stream()
                .mapToInt(v -> v.getSold() != null ? v.getSold() : 0)
                .sum();
    }
}
