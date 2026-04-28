package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.repository.CouponRepository;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    // =====================================================
    // VALIDATE COUPON (read-only, no side effects)
    // =====================================================
    public Coupon validateCoupon(String code, Double orderTotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.trim().toUpperCase()).orElse(null);
        if (coupon == null) {
            return null;
        }
        return coupon.isValid(orderTotal) ? coupon : null;
    }

    // =====================================================
    // APPLY COUPON — trừ discount + tăng usageCount
    // Gọi trong cùng transaction với checkout
    // =====================================================
    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank()) {
            return orderTotal;
        }

        Coupon coupon = couponRepository
                .findByCodeAndActiveTrue(code.trim().toUpperCase())
                .orElse(null);

        if (coupon == null || !coupon.isValid(orderTotal.doubleValue())) {
            log.warn("Coupon invalid or expired at apply time | code={}", code);
            return orderTotal;
        }

        BigDecimal discounted = BigDecimal.valueOf(
                coupon.applyDiscount(orderTotal.doubleValue())
        ).setScale(0, RoundingMode.HALF_UP);

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        log.info("Coupon applied | code={} | original={} | discounted={}", code, orderTotal, discounted);
        return discounted;
    }
}
