package com.shop.clothingstore.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.ProductResponse;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.repository.UserRepository;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsApiController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;

    public AnalyticsApiController(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
    }

    // =====================================================
    // GET /api/analytics/top-products?limit=10
    // =====================================================
    @GetMapping("/top-products")
    public ResponseEntity<List<ProductResponse>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {

        List<ProductResponse> result = productRepository
                .findTopSellingProducts(PageRequest.of(0, Math.min(limit, 50)))
                .stream()
                .map(ProductResponse::summary)
                .toList();

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // GET /api/analytics/trending (newest products + best sellers)
    // =====================================================
    @GetMapping("/trending")
    public ResponseEntity<List<ProductResponse>> trending(
            @RequestParam(defaultValue = "8") int limit) {

        // Trending = newest products with at least one sale
        List<ProductResponse> result = productRepository
                .findBestSellers(PageRequest.of(0, Math.min(limit, 50)))
                .stream()
                .map(ProductResponse::summary)
                .toList();

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // GET /api/analytics/overview (overall KPIs)
    // =====================================================
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {

        Map<String, Object> data = new HashMap<>();

        data.put("totalOrders", orderRepository.count());
        data.put("completedOrders", orderRepository.countByStatus(OrderStatus.COMPLETED));
        data.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        data.put("totalRevenue", orderRepository.getTotalAmountByStatus(OrderStatus.COMPLETED));
        data.put("totalUsers", userRepository.count());
        data.put("totalProducts", productRepository.count());
        Long totalStock = variantRepository.getTotalStock();
        Long totalSold = variantRepository.getTotalSold();
        data.put("totalStock", totalStock != null ? totalStock : 0L);
        data.put("totalSold", totalSold != null ? totalSold : 0L);

        return ResponseEntity.ok(data);
    }
}
