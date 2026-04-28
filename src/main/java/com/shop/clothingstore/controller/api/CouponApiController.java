package com.shop.clothingstore.controller.api;

import java.math.BigDecimal;

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
    // Body: { "code": "SUMMER20", "orderTotal": 500000 }
    // FIX: dùng POST thay GET để tránh lộ code trong URL/log
    // FIX: delegate qua CouponService thay inject repo trực tiếp
    // FIX: validate coupon.isValid() trước khi trả về
    // =====================================================
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateRequest request) {

        log.debug("Validating coupon | code={} | orderTotal={}", request.getCode(), request.getOrderTotal());

        Coupon coupon = couponService.validateCoupon(request.getCode(), request.getOrderTotal());

        if (coupon == null) {
            return ResponseEntity.ok(new CouponValidationResponse(
                    false,
                    "Mã giảm giá không hợp lệ hoặc đã hết hạn",
                    null,
                    null,
                    null
            ));
        }

        Double discountedTotal = coupon.applyDiscount(request.getOrderTotal());
        Double savedAmount = request.getOrderTotal() - discountedTotal;

        return ResponseEntity.ok(new CouponValidationResponse(
                true,
                "Mã giảm giá hợp lệ",
                coupon.getDiscountValue(),
                BigDecimal.valueOf(discountedTotal).setScale(0, java.math.RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(savedAmount).setScale(0, java.math.RoundingMode.HALF_UP).doubleValue()
        ));
    }

    // =====================================================
    // Request DTO
    // =====================================================
    @Data
    public static class ValidateRequest {

        @NotBlank(message = "Mã coupon không được trống")
        private String code;

        @NotNull(message = "Tổng tiền không được null")
        @PositiveOrZero
        private Double orderTotal;
    }

    // =====================================================
    // Response DTO
    // =====================================================
    public record CouponValidationResponse(
            boolean valid,
            String message,
            Double discountValue,
            Double discountedTotal,
            Double savedAmount
            ) {

    }
}
