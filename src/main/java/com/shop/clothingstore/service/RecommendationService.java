package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductRepository;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public RecommendationService(
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    // =====================================================
    // 1. CONTENT-BASED: similar products (same category, ±30% price range)
    // =====================================================
    public List<Product> getSimilarProducts(Long productId, int limit) {

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }

        Long subCategoryId = product.getSubCategory().getId();
        Long categoryId = product.getSubCategory().getCategory().getId();

        // getMinPrice() returns BigDecimal — no Double anywhere in this method
        BigDecimal minPrice = product.getMinPrice();

        // Price range ±30% using BigDecimal arithmetic — no * operator
        BigDecimal priceLow  = minPrice.multiply(new BigDecimal("0.7"));
        BigDecimal priceHigh = minPrice.multiply(new BigDecimal("1.3"));

        List<Product> sameSub = productRepository.findSimilarBySubCategory(
                subCategoryId, productId, PageRequest.of(0, limit * 2));

        if (sameSub.size() < limit) {
            List<Product> sameCat = productRepository.findSimilarByCategory(
                    categoryId, productId, PageRequest.of(0, limit * 2));

            Set<Long> existingIds = sameSub.stream()
                    .map(Product::getId).collect(Collectors.toSet());

            for (Product p : sameCat) {
                if (!existingIds.contains(p.getId())) {
                    sameSub.add(p);
                }
            }
        }

        // Filter by price range using BigDecimal.compareTo() — no >= or <= on BigDecimal
        List<Product> filtered = sameSub.stream()
                .filter(p -> {
                    BigDecimal pMin = p.getMinPrice();
                    if (pMin == null) {
                        pMin = BigDecimal.ZERO;
                    }
                    return pMin.compareTo(priceLow)  >= 0
                        && pMin.compareTo(priceHigh) <= 0;
                })
                .limit(limit)
                .toList();

        log.debug("Similar products for id={} | found={} | afterPriceFilter={}",
                productId, sameSub.size(), filtered.size());
        return filtered;
    }

    // =====================================================
    // 2. COLLABORATIVE: users who bought this also bought
    // =====================================================
    public List<Product> getCollaborativeRecommendations(User user, int limit) {

        if (user == null) {
            return List.of();
        }

        List<Order> userOrders = orderRepository.findByActor(user);
        Set<Long> userVariantIds = new HashSet<>();

        for (Order order : userOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getVariantId() != null) {
                        userVariantIds.add(item.getVariantId());
                    }
                }
            }
        }

        if (userVariantIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(90);
        List<Order> allOrders = orderRepository.findByStatusSince(OrderStatus.COMPLETED, since);
        Map<Long, Set<Long>> otherUserVariants = new HashMap<>();

        for (Order order : allOrders) {
            if (order.getActor() == null || order.getActor().getId().equals(user.getId())) {
                continue;
            }
            if (order.getItems() == null) {
                continue;
            }

            Long otherUserId = order.getActor().getId();
            Set<Long> variants = otherUserVariants
                    .computeIfAbsent(otherUserId, k -> new HashSet<>());

            for (OrderItem item : order.getItems()) {
                if (item.getVariantId() != null) {
                    variants.add(item.getVariantId());
                }
            }
        }

        Set<Long> recommendedVariantIds = new LinkedHashSet<>();

        for (Map.Entry<Long, Set<Long>> entry : otherUserVariants.entrySet()) {
            Set<Long> otherVariants = entry.getValue();

            long overlap = otherVariants.stream()
                    .filter(userVariantIds::contains)
                    .count();

            if (overlap > 0) {
                for (Long variantId : otherVariants) {
                    if (variantId != null && !userVariantIds.contains(variantId)) {
                        recommendedVariantIds.add(variantId);
                    }
                }
            }
        }

        if (recommendedVariantIds.isEmpty()) {
            return List.of();
        }

        List<Product> result = new ArrayList<>();
        Set<Long> addedProductIds = new HashSet<>();

        for (Long variantId : recommendedVariantIds) {
            if (result.size() >= limit) {
                break;
            }

            productRepository.findByVariantId(variantId).ifPresent(p -> {
                if (!addedProductIds.contains(p.getId()) && p.isActive()) {
                    result.add(p);
                    addedProductIds.add(p.getId());
                }
            });
        }

        log.debug("Collaborative recommendations for user={} | found={}",
                user.getEmail(), result.size());
        return result;
    }

    // =====================================================
    // 3. HYBRID: collaborative + best sellers fallback
    // =====================================================
    public List<Product> getRecommendations(User user, int limit) {

        Set<Long> addedIds = new LinkedHashSet<>();
        List<Product> result = new ArrayList<>();

        if (user != null) {
            for (Product p : getCollaborativeRecommendations(user, limit)) {
                if (addedIds.add(p.getId())) {
                    result.add(p);
                }
            }
        }

        if (result.size() < limit) {
            for (Product p : productRepository.findBestSellers(PageRequest.of(0, limit))) {
                if (addedIds.add(p.getId())) {
                    result.add(p);
                }
            }
        }

        log.info("Hybrid recommendations | user={} | total={}",
                user != null ? user.getEmail() : "GUEST", result.size());

        return result.stream().limit(limit).toList();
    }
}
