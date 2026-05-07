package com.shop.clothingstore.controller.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.exception.InvalidOrderStateException;
import com.shop.clothingstore.service.OrderService;
import com.shop.clothingstore.service.ProductService;
import com.shop.clothingstore.service.UserService;

/**
 * Admin REST API — AJAX endpoints for quick actions that don't require full page reload.
 * All endpoints require ADMIN role.
 *
 * Endpoints:
 *   GET  /api/admin/stats/summary          — KPI snapshot (orders, revenue, users)
 *   POST /api/admin/orders/{id}/status     — Quick order status update (returns JSON)
 *   POST /api/admin/products/bulk-status   — Bulk activate / deactivate products
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    private final OrderService   orderService;
    private final ProductService productService;
    private final UserService    userService;

    public AdminApiController(
            OrderService orderService,
            ProductService productService,
            UserService userService) {
        this.orderService   = orderService;
        this.productService = productService;
        this.userService    = userService;
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/admin/stats/summary
    // Lightweight KPI snapshot for dashboard AJAX refresh.
    // ─────────────────────────────────────────────────────────
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        long totalOrders  = orderService.getAllOrders(
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long totalUsers   = userService.findAll(
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long pendingOrders = orderService.searchOrders(
                null, "PENDING", null, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        return ResponseEntity.ok(Map.of(
                "totalOrders",   totalOrders,
                "totalUsers",    totalUsers,
                "pendingOrders", pendingOrders
        ));
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/orders/{id}/status
    // Body: { "status": "PROCESSING" }
    // Returns: { "id": 1, "newStatus": "PROCESSING", "success": true }
    //
    // Allows admin to change order status directly from the order
    // list via AJAX without navigating to the detail page.
    // ─────────────────────────────────────────────────────────
    @PostMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'status' field"));
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status value: " + statusStr));
        }

        try {
            Order updated = orderService.updateOrderStatus(id, newStatus);
            log.info("Admin API: order #{} status → {}", id, newStatus);
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "id",        updated.getId(),
                    "newStatus", updated.getStatus().name()
            ));

        } catch (InvalidOrderStateException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Admin API: failed to update order #{} status", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/products/bulk-status
    // Body: { "ids": [1, 2, 3], "active": true }
    // Returns: { "updated": 3, "success": true }
    //
    // Bulk activate or deactivate a list of products.
    // Replaces the need to edit each product individually.
    // ─────────────────────────────────────────────────────────
    @Transactional
    @PostMapping("/products/bulk-status")
    public ResponseEntity<?> bulkProductStatus(
            @RequestBody Map<String, Object> body) {

        Object idsObj    = body.get("ids");
        Object activeObj = body.get("active");

        if (!(idsObj instanceof List<?> rawIds) || activeObj == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Required fields: 'ids' (list) and 'active' (boolean)"));
        }

        boolean active;
        try {
            active = Boolean.parseBoolean(activeObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'active' must be true or false"));
        }

        List<Long> ids = rawIds.stream()
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).longValue())
                .toList();

        if (ids.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Product list is empty"));
        }

        int updated = 0;
        for (Long productId : ids) {
            try {
                productService.findById(productId).ifPresent(product -> {
                    product.setActive(active);
                    productService.save(product);
                });
                updated++;
            } catch (Exception e) {
                log.warn("Admin API: failed to update product #{} active={}", productId, active, e);
            }
        }

        log.info("Admin API bulk-status: {} products set active={}", updated, active);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updated", updated,
                "active",  active
        ));
    }
}
