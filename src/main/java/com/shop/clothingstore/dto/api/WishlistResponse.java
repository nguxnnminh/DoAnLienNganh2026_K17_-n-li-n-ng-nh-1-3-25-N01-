package com.shop.clothingstore.dto.api;

import java.math.BigDecimal;

import com.shop.clothingstore.entity.WishlistItem;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal minPrice;
    private String primaryImageUrl;

    public static WishlistResponse from(WishlistItem item) {
        String imageUrl = null;
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            imageUrl = item.getProduct().getImages().stream()
                    .filter(img -> img.isPrimaryImage())
                    .findFirst()
                    .or(() -> item.getProduct().getImages().stream().findFirst())
                    .map(img -> img.getImageUrl())
                    .orElse(null);
        }

        return new WishlistResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSlug(),
                item.getProduct().getMinPrice(),
                imageUrl
        );
    }
}
