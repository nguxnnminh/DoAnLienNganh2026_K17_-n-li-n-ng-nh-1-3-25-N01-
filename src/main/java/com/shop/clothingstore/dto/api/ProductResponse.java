package com.shop.clothingstore.dto.api;

import java.util.List;

import com.shop.clothingstore.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private boolean active;
    private Double minPrice;
    private Integer totalStock;
    private Integer totalSold;
    private String categoryName;
    private String subCategoryName;
    private List<VariantInfo> variants;
    private List<ImageInfo> images;

    @Data
    @AllArgsConstructor
    public static class VariantInfo {

        private Long id;
        private String size;
        private String color;
        private Double price;
        private Integer stock;
    }

    @Data
    @AllArgsConstructor
    public static class ImageInfo {

        private Long id;
        private String imageUrl;
        private boolean primaryImage;
    }

    public static ProductResponse from(Product p) {

        List<VariantInfo> variants = p.getProductVariants().stream()
                .map(v -> new VariantInfo(
                v.getId(),
                v.getSize(),
                v.getColor(),
                v.getPrice(),
                v.getStock()
        ))
                .toList();

        List<ImageInfo> images = p.getImages().stream()
                .map(img -> new ImageInfo(
                img.getId(),
                img.getImageUrl(),
                img.isPrimaryImage()
        ))
                .toList();

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.isActive(),
                p.getMinPrice(),
                p.getTotalStock(),
                p.getTotalSold(),
                p.getSubCategory().getCategory().getName(),
                p.getSubCategory().getName(),
                variants,
                images
        );
    }

    // Phiên bản rút gọn cho danh sách
    public static ProductResponse summary(Product p) {

        String primaryImg = p.getImages().stream()
                .filter(img -> img.isPrimaryImage())
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(p.getImages().isEmpty() ? null
                        : p.getImages().iterator().next().getImageUrl());

        List<ImageInfo> images = primaryImg != null
                ? List.of(new ImageInfo(null, primaryImg, true))
                : List.of();

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                null,
                p.isActive(),
                p.getMinPrice(),
                p.getTotalStock(),
                p.getTotalSold(),
                p.getSubCategory().getCategory().getName(),
                p.getSubCategory().getName(),
                List.of(),
                images
        );
    }
}
