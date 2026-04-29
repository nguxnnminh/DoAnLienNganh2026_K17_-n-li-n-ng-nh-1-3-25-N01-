package com.shop.clothingstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.entity.Coupon.DiscountType;
import com.shop.clothingstore.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentageCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = new Coupon();
        percentageCoupon.setCode("SAVE20");
        percentageCoupon.setDiscountType(DiscountType.PERCENTAGE);
        percentageCoupon.setDiscountValue(new BigDecimal("20")); // 20%
        percentageCoupon.setMinOrderAmount(new BigDecimal("100000"));
        percentageCoupon.setUsageLimit(100);
        percentageCoupon.setUsageCount(0);
        percentageCoupon.setActive(true);

        fixedCoupon = new Coupon();
        fixedCoupon.setCode("FLAT50K");
        fixedCoupon.setDiscountType(DiscountType.FIXED);
        fixedCoupon.setDiscountValue(new BigDecimal("50000"));
        fixedCoupon.setMinOrderAmount(null);
        fixedCoupon.setUsageLimit(null);
        fixedCoupon.setUsageCount(0);
        fixedCoupon.setActive(true);
    }

    // =====================================================
    // validateCoupon (read-only, no side effects)
    // =====================================================

    @Test
    void validateCoupon_validPercentageCoupon_returnsCoupon() {
        when(couponRepository.findByCodeAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        Coupon result = couponService.validateCoupon("SAVE20", new BigDecimal("500000"));

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SAVE20");
    }

    @Test
    void validateCoupon_nullCode_returnsNull() {
        Coupon result = couponService.validateCoupon(null, new BigDecimal("500000"));
        assertThat(result).isNull();
        verify(couponRepository, never()).findByCodeAndActiveTrue(any());
    }

    @Test
    void validateCoupon_blankCode_returnsNull() {
        Coupon result = couponService.validateCoupon("   ", new BigDecimal("500000"));
        assertThat(result).isNull();
    }

    @Test
    void validateCoupon_unknownCode_returnsNull() {
        when(couponRepository.findByCodeAndActiveTrue("UNKNOWN"))
                .thenReturn(Optional.empty());

        Coupon result = couponService.validateCoupon("UNKNOWN", new BigDecimal("500000"));
        assertThat(result).isNull();
    }

    @Test
    void validateCoupon_orderBelowMinimum_returnsNull() {
        when(couponRepository.findByCodeAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        // minOrderAmount is 100,000 but order is only 50,000
        Coupon result = couponService.validateCoupon("SAVE20", new BigDecimal("50000"));
        assertThat(result).isNull();
    }

    @Test
    void validateCoupon_expiredCoupon_returnsNull() {
        percentageCoupon.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        Coupon result = couponService.validateCoupon("SAVE20", new BigDecimal("500000"));
        assertThat(result).isNull();
    }

    @Test
    void validateCoupon_usageLimitReached_returnsNull() {
        percentageCoupon.setUsageLimit(10);
        percentageCoupon.setUsageCount(10); // exhausted
        when(couponRepository.findByCodeAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        Coupon result = couponService.validateCoupon("SAVE20", new BigDecimal("500000"));
        assertThat(result).isNull();
    }

    // =====================================================
    // applyCoupon (with lock, increments usageCount)
    // =====================================================

    @Test
    void applyCoupon_nullCode_returnsOriginalTotal() {
        BigDecimal total = new BigDecimal("500000");
        BigDecimal result = couponService.applyCoupon(null, total);
        assertThat(result).isEqualByComparingTo(total);
        verify(couponRepository, never()).findByCodeForUpdate(any());
    }

    @Test
    void applyCoupon_blankCode_returnsOriginalTotal() {
        BigDecimal total = new BigDecimal("500000");
        BigDecimal result = couponService.applyCoupon("  ", total);
        assertThat(result).isEqualByComparingTo(total);
    }

    @Test
    void applyCoupon_validPercentageCoupon_returnsDiscountedAmount() {
        when(couponRepository.findByCodeForUpdate("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        BigDecimal result = couponService.applyCoupon("SAVE20", new BigDecimal("500000"));

        // 20% off 500,000 = 400,000
        assertThat(result).isEqualByComparingTo("400000");
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(1);
        verify(couponRepository).save(percentageCoupon);
    }

    @Test
    void applyCoupon_validFixedCoupon_returnsDiscountedAmount() {
        when(couponRepository.findByCodeForUpdate("FLAT50K"))
                .thenReturn(Optional.of(fixedCoupon));

        BigDecimal result = couponService.applyCoupon("FLAT50K", new BigDecimal("500000"));

        // 500,000 - 50,000 = 450,000
        assertThat(result).isEqualByComparingTo("450000");
        assertThat(fixedCoupon.getUsageCount()).isEqualTo(1);
    }

    @Test
    void applyCoupon_fixedDiscount_cannotGoNegative() {
        fixedCoupon.setDiscountValue(new BigDecimal("9999999"));
        when(couponRepository.findByCodeForUpdate("FLAT50K"))
                .thenReturn(Optional.of(fixedCoupon));

        BigDecimal result = couponService.applyCoupon("FLAT50K", new BigDecimal("100"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void applyCoupon_couponNotFound_throwsIllegalState() {
        when(couponRepository.findByCodeForUpdate("GHOST"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.applyCoupon("GHOST", new BigDecimal("500000")))
                .isInstanceOf(IllegalStateException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    void applyCoupon_couponExpiredAtApplyTime_throwsIllegalState() {
        // Valid at validate time, expired by apply time (race condition scenario)
        percentageCoupon.setExpiryDate(LocalDateTime.now().minusSeconds(1));
        when(couponRepository.findByCodeForUpdate("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.applyCoupon("SAVE20", new BigDecimal("500000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SAVE20");

        // usageCount must NOT be incremented when invalid
        assertThat(percentageCoupon.getUsageCount()).isEqualTo(0);
        verify(couponRepository, never()).save(any());
    }

    @Test
    void applyCoupon_incrementsUsageCount() {
        percentageCoupon.setUsageCount(5);
        when(couponRepository.findByCodeForUpdate("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        couponService.applyCoupon("SAVE20", new BigDecimal("500000"));

        assertThat(percentageCoupon.getUsageCount()).isEqualTo(6);
    }

    // =====================================================
    // Coupon entity — discount math
    // =====================================================

    @Test
    void coupon_percentageDiscount_cappedAt100Percent() {
        // discountValue > 100 should not produce negative total
        percentageCoupon.setDiscountValue(new BigDecimal("150"));
        BigDecimal result = percentageCoupon.applyDiscount(new BigDecimal("500000"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void coupon_isValid_inactiveCoupon_returnsFalse() {
        percentageCoupon.setActive(false);
        assertThat(percentageCoupon.isValid(new BigDecimal("500000"))).isFalse();
    }

    @Test
    void coupon_isValid_noMinimum_alwaysValid() {
        fixedCoupon.setMinOrderAmount(null);
        assertThat(fixedCoupon.isValid(new BigDecimal("1"))).isTrue();
    }
}
