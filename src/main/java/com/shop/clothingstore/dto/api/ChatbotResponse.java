package com.shop.clothingstore.dto.api;

import java.math.BigDecimal;
import java.util.List;

import com.shop.clothingstore.entity.Product;

public class ChatbotResponse {

    private String message;
    private List<ProductSummary> products;

    public static ChatbotResponse text(String message) {
        ChatbotResponse r = new ChatbotResponse();
        r.message = message;
        r.products = List.of();
        return r;
    }

    public static ChatbotResponse withProducts(String message, List<Product> products) {
        ChatbotResponse r = new ChatbotResponse();
        r.message = message;
        r.products = products == null ? List.of() : products.stream().map(ProductSummary::from).toList();
        return r;
    }

    public String getMessage() {
        return message;
    }

    public List<ProductSummary> getProducts() {
        return products;
    }

    public static class ProductSummary {

        private Long id;
        private String name;
        private String slug;
        private BigDecimal minPrice;
        private String imageUrl;

        public static ProductSummary from(Product p) {
            ProductSummary s = new ProductSummary();
            s.id = p.getId();
            s.name = p.getName();
            s.slug = p.getSlug();
            s.minPrice = p.getMinPrice();
            if (p.getImages() == null) {
                s.imageUrl = null;
            } else {
                s.imageUrl = p.getImages().stream()
                        .filter(img -> img.isPrimaryImage())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(p.getImages().isEmpty() ? null : p.getImages().iterator().next().getImageUrl());
            }
            return s;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }

        public BigDecimal getMinPrice() {
            return minPrice;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }
}

