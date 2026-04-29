package com.shop.clothingstore.controller.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.service.CouponService;

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

    public CouponApiController(CouponService couponService) {
        this.couponService = couponService;
    }

    // =====================================================
    // POST /api/coupons/validate
    // Read-only preview — does NOT increment usageCount.
    // Uses BigDecimal for all monetary values.
    // =====================================================
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateRequest request) {

        log.debug("Validating coupon | code={} | orderTotal={}", request.getCode(), request.getOrderTotal());

        Coupon coupon = couponService.validateCoupon(request.getCode(), request.getOrderTotal());

        if (coupon == null) {
            return ResponseEntity.ok(new CouponValidationResponse(
                    false, "Ma giam gia khong hop le hoac da het han", null, null, null));
        }

        BigDecimal discountedTotal = coupon.applyDiscount(request.getOrderTotal());
        BigDecimal savedAmount = request.getOrderTotal().subtract(discountedTotal)
                .setScale(0, RoundingMode.HALF_UP);

        return ResponseEntity.ok(new CouponValidationResponse(
                true,
                "Ma giam gia hop le",
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
