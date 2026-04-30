package com.shop.clothingstore.controller.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.CouponService;
import com.shop.clothingstore.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@RestController
@RequestMapping("/api/coupons")
public class CouponApiController {

    private static final Logger log = LoggerFactory.getLogger(CouponApiController.class);

    private final CouponService couponService;
    private final UserService userService;

    public CouponApiController(CouponService couponService, UserService userService) {
        this.couponService = couponService;
        this.userService = userService;
    }

    // =====================================================
    // POST /api/coupons/validate
    // Read-only preview — does NOT increment usageCount.
    // Uses BigDecimal for all monetary values.
    // Now supports user-specific coupon validation.
    // =====================================================
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateRequest request,
            Principal principal) {

        log.debug("Validating coupon | code={} | orderTotal={}", request.getCode(), request.getOrderTotal());

        // Resolve current user (may be null for guest)
        User user = null;
        if (principal != null) {
            user = userService.findByEmail(principal.getName()).orElse(null);
        }

        Coupon coupon = couponService.validateCoupon(request.getCode(), request.getOrderTotal(), user);

        if (coupon == null) {
            return ResponseEntity.ok(new CouponValidationResponse(
                    false, "Mã giảm giá không hợp lệ hoặc đã hết hạn", null, null, null));
        }

        BigDecimal discountedTotal = coupon.applyDiscount(request.getOrderTotal());
        BigDecimal savedAmount = request.getOrderTotal().subtract(discountedTotal)
                .setScale(0, RoundingMode.HALF_UP);

        return ResponseEntity.ok(new CouponValidationResponse(
                true,
                "Mã giảm giá hợp lệ",
                coupon.getDiscountValue(),
                discountedTotal,
                savedAmount
        ));
    }

    @Data
    public static class ValidateRequest {

        @NotBlank(message = "Ma coupon khong duoc trong")
        private String code;

        @NotNull(message = "Tong tien khong duoc null")
        @PositiveOrZero
        private BigDecimal orderTotal;
    }

    public record CouponValidationResponse(
            boolean valid,
            String message,
            BigDecimal discountValue,
            BigDecimal discountedTotal,
            BigDecimal savedAmount) {
    }
}
