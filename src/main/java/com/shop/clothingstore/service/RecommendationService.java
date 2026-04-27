package com.shop.clothingstore.service;

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
    // 1. CONTENT-BASED: sản phẩm tương tự (cùng category, price range)
    // =====================================================
    public List<Product> getSimilarProducts(Long productId, int limit) {

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }

        Long subCategoryId = product.getSubCategory().getId();
        Long categoryId = product.getSubCategory().getCategory().getId();
        Double minPrice = product.getMinPrice();

        // Khoảng giá ±30%
        double priceLow = minPrice * 0.7;
        double priceHigh = minPrice * 1.3;

        // Tìm sản phẩm cùng subcategory trước
        List<Product> sameSub = productRepository.findSimilarBySubCategory(
                subCategoryId, productId, PageRequest.of(0, limit * 2));

        // Nếu chưa đủ, mở rộng ra cùng category
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

        // Filter theo khoảng giá ±30%
        List<Product> filtered = sameSub.stream()
                .filter(p -> {
                    double pMin = p.getMinPrice() != null ? p.getMinPrice() : 0.0;
                    return pMin >= priceLow && pMin <= priceHigh;
                })
                .limit(limit)
                .toList();

        log.debug("Similar products for id={} | found={} | afterPriceFilter={}",
                productId, sameSub.size(), filtered.size());
        return filtered;
    }

    // =====================================================
    // 2. COLLABORATIVE: user đã mua → user khác cũng mua → gợi ý
    // =====================================================
    public List<Product> getCollaborativeRecommendations(User user, int limit) {

        if (user == null) {
            return List.of();
        }

        // Bước 1: Lấy product IDs user đã mua (từ order items → variantId → product)
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

        // Bước 2: Tìm user khác đã mua cùng variant (chỉ trong 90 ngày gần nhất)
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        List<Order> allOrders = orderRepository.findByStatusSince(OrderStatus.COMPLETED, since);
        Map<Long, Set<Long>> otherUserVariants = new HashMap<>(); // userId → variantIds

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

        // Bước 3: Tìm user tương tự (có ít nhất 1 variant chung)
        Set<Long> recommendedVariantIds = new LinkedHashSet<>();

        for (Map.Entry<Long, Set<Long>> entry : otherUserVariants.entrySet()) {
            Set<Long> otherVariants = entry.getValue();

            // Đếm overlap
            long overlap = otherVariants.stream()
                    .filter(userVariantIds::contains)
                    .count();

            if (overlap > 0) {
                // Thêm variants mà user CHƯA mua
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

        // Bước 4: Load products từ variantIds
        List<Product> result = new ArrayList<>();
        Set<Long> addedProductIds = new HashSet<>();

        for (Long variantId : recommendedVariantIds) {
            if (result.size() >= limit) {
                break;
            }

            productRepository.findByVariantId(variantId).ifPresent(product -> {
                if (!addedProductIds.contains(product.getId()) && product.isActive()) {
                    result.add(product);
                    addedProductIds.add(product.getId());
                }
            });
        }

        log.debug("Collaborative recommendations for user={} | found={}",
                user.getEmail(), result.size());
        return result;
    }

    // =====================================================
    // 3. HYBRID: combine content-based + collaborative + best sellers
    // =====================================================
    public List<Product> getRecommendations(User user, int limit) {

        Set<Long> addedIds = new LinkedHashSet<>();
        List<Product> result = new ArrayList<>();

        // Ưu tiên 1: Collaborative (nếu có user)
        if (user != null) {
            List<Product> collaborative = getCollaborativeRecommendations(user, limit);
            for (Product p : collaborative) {
                if (addedIds.add(p.getId())) {
                    result.add(p);
                }
            }
        }

        // Ưu tiên 2: Best sellers (fallback)
        if (result.size() < limit) {
            List<Product> bestSellers = productRepository.findBestSellers(
                    PageRequest.of(0, limit));
            for (Product p : bestSellers) {
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
