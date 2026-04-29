package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    // ADMIN CRUD
    // =====================================================
    public List<Coupon> findAll() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> findById(Long id) {
        return couponRepository.findById(id);
    }

    @SuppressWarnings("null")
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    public boolean existsByCode(String code) {
        return couponRepository.findByCodeAndActiveTrue(code.trim().toUpperCase()).isPresent()
                || couponRepository.findAll().stream()
                        .anyMatch(c -> c.getCode().equalsIgnoreCase(code.trim()));
    }

    // =====================================================
    // VALIDATE COUPON (read-only, no side effects, no lock)
    // Used by the UI preview endpoint only.
    // =====================================================
    public Coupon validateCoupon(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository
                .findByCodeAndActiveTrue(code.trim().toUpperCase())
                .orElse(null);
        if (coupon == null) {
            return null;
        }
        return coupon.isValid(orderTotal) ? coupon : null;
    }

    // =====================================================
    // APPLY COUPON — pessimistic lock + increment usageCount.
    // MUST be called inside the same @Transactional as checkout.
    //
    // FIX: throws IllegalStateException if coupon is invalid at
    // apply-time instead of silently returning full price.
    // This prevents the user being charged without knowing the
    // discount was not applied (race condition or expiry window).
    // =====================================================
    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank()) {
            return orderTotal;
        }

        // PESSIMISTIC LOCK — blocks concurrent transactions until this one commits
        Coupon coupon = couponRepository
                .findByCodeForUpdate(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalStateException(
                "Ma giam gia '" + code.trim().toUpperCase() + "' khong hop le."));

        if (!coupon.isValid(orderTotal)) {
            log.warn("Coupon invalid at apply time | code={} | orderTotal={}", code, orderTotal);
            throw new IllegalStateException(
                    "Ma giam gia '" + code.trim().toUpperCase()
                    + "' da het han hoac khong dap ung dieu kien don hang. "
                    + "Vui long xoa ma va thu lai.");
        }

        BigDecimal discounted = coupon.applyDiscount(orderTotal);

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        log.info("Coupon applied | code={} | original={} | discounted={}", code, orderTotal, discounted);
        return discounted;
    }
}
