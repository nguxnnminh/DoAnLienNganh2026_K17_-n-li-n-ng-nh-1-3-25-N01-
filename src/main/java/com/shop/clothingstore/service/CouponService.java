package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.entity.UserCoupon;
import com.shop.clothingstore.repository.CouponRepository;
import com.shop.clothingstore.repository.UserCouponRepository;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private static final BigDecimal WELCOME_DISCOUNT = new BigDecimal("100000");
    private static final BigDecimal WELCOME_MIN_ORDER = new BigDecimal("200000");
    private static final int WELCOME_EXPIRY_DAYS = 30;

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponService(CouponRepository couponRepository,
                         UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
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

    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    public boolean existsByCode(String code) {
        return couponRepository.existsByCodeIgnoreCase(code.trim());
    }

    // =====================================================
    // WELCOME COUPON — auto-created for new users
    // =====================================================
    @Transactional
    public void createWelcomeCouponForUser(User user) {
        // Generate unique code: WELCOME-<short uuid>
        String code = "WELCOME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription("Welcome new member! Get 100,000₫ off your first order.");
        coupon.setDiscountType(Coupon.DiscountType.FIXED);
        coupon.setDiscountValue(WELCOME_DISCOUNT);
        coupon.setMinOrderAmount(WELCOME_MIN_ORDER);
        coupon.setStartDate(LocalDateTime.now());
        coupon.setExpiryDate(LocalDateTime.now().plusDays(WELCOME_EXPIRY_DAYS));
        coupon.setUsageLimit(1);
        coupon.setUsageCount(0);
        coupon.setActive(true);
        coupon.setUserSpecific(true);

        coupon = couponRepository.save(coupon);

        // Link coupon to user
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCoupon.setUsed(false);
        userCouponRepository.save(userCoupon);

        log.info("Welcome coupon created | code={} | userId={} | email={}",
                code, user.getId(), user.getEmail());
    }

    // =====================================================
    // REFERRAL: cấp 1 coupon user-specific (giảm tiền cố định) cho user
    // Dùng khi thưởng giới thiệu (cả người giới thiệu + người được giới thiệu)
    // =====================================================
    @Transactional
    public void grantFixedCoupon(User user, String prefix, BigDecimal amount,
            BigDecimal minOrder, int validityDays, String description) {
        String code = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(description);
        coupon.setDiscountType(Coupon.DiscountType.FIXED);
        coupon.setDiscountValue(amount);
        coupon.setMinOrderAmount(minOrder);
        coupon.setStartDate(LocalDateTime.now());
        coupon.setExpiryDate(LocalDateTime.now().plusDays(validityDays));
        coupon.setUsageLimit(1);
        coupon.setUsageCount(0);
        coupon.setActive(true);
        coupon.setUserSpecific(true);
        coupon = couponRepository.save(coupon);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCoupon.setUsed(false);
        userCouponRepository.save(userCoupon);

        log.info("Granted coupon | code={} | userId={} | amount={}", code, user.getId(), amount);
    }

    // =====================================================
    // USER-FACING: Get all coupons for "My Coupons" page
    // Returns both public + user-specific coupons
    // =====================================================
    public List<CouponDisplayDTO> getAllCouponsForUser(User user) {
        List<CouponDisplayDTO> result = new ArrayList<>();

        // 1. User-specific coupons (from user_coupons table)
        List<UserCoupon> userCoupons = userCouponRepository.findByUser(user);
        for (UserCoupon uc : userCoupons) {
            Coupon c = uc.getCoupon();
            CouponDisplayDTO dto = new CouponDisplayDTO();
            dto.setCode(c.getCode());
            dto.setDescription(c.getDescription());
            dto.setDiscountType(c.getDiscountType());
            dto.setDiscountValue(c.getDiscountValue());
            dto.setMinOrderAmount(c.getMinOrderAmount());
            dto.setExpiryDate(c.getExpiryDate());
            dto.setStartDate(c.getStartDate());
            dto.setUsed(uc.isUsed());
            dto.setExpired(c.isExpired());
            dto.setActive(c.isActive());
            dto.setUsageLimitReached(c.isUsageLimitReached());
            dto.setUserSpecific(true);
            result.add(dto);
        }

        // 2. Public coupons (not user-specific)
        List<Coupon> publicCoupons = couponRepository.findByActiveTrueAndUserSpecificFalse();
        for (Coupon c : publicCoupons) {
            // Check if user has already used this public coupon
            boolean used = userCouponRepository.existsByUserAndCouponAndUsedTrue(user, c);

            CouponDisplayDTO dto = new CouponDisplayDTO();
            dto.setCode(c.getCode());
            dto.setDescription(c.getDescription());
            dto.setDiscountType(c.getDiscountType());
            dto.setDiscountValue(c.getDiscountValue());
            dto.setMinOrderAmount(c.getMinOrderAmount());
            dto.setExpiryDate(c.getExpiryDate());
            dto.setStartDate(c.getStartDate());
            dto.setUsed(used);
            dto.setExpired(c.isExpired());
            dto.setActive(c.isActive());
            dto.setUsageLimitReached(c.isUsageLimitReached());
            dto.setUserSpecific(false);
            result.add(dto);
        }

        return result;
    }

    // =====================================================
    // USER-FACING: Get available coupons for checkout
    // Only returns coupons that are valid right now
    // =====================================================
    public List<CouponDisplayDTO> getAvailableCouponsForUser(User user, BigDecimal orderTotal) {
        List<CouponDisplayDTO> all = getAllCouponsForUser(user);

        // Filter to only usable ones
        List<CouponDisplayDTO> available = all.stream()
                .filter(dto -> !dto.isUsed()
                        && !dto.isExpired()
                        && dto.isActive()
                        && !dto.isUsageLimitReached()
                        && (dto.getStartDate() == null || !dto.getStartDate().isAfter(LocalDateTime.now()))
                        && (dto.getMinOrderAmount() == null
                                || orderTotal.compareTo(dto.getMinOrderAmount()) >= 0))
                .toList();

        // Sort: highest discount first, then soonest expiry
        return available.stream()
                .sorted(Comparator
                        // Fixed amount first (easier to compare), then by value desc
                        .comparing((CouponDisplayDTO dto) -> dto.calculateDiscountAmount(orderTotal))
                        .reversed()
                        // Then soonest expiry
                        .thenComparing(dto -> dto.getExpiryDate() != null
                                ? dto.getExpiryDate()
                                : LocalDateTime.MAX))
                .toList();
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

    /**
     * Validate coupon with user context — checks user-specific restrictions.
     */
    public Coupon validateCoupon(String code, BigDecimal orderTotal, User user) {
        Coupon coupon = validateCoupon(code, orderTotal);
        if (coupon == null) {
            return null;
        }

        // If user-specific, check if user has this coupon assigned
        if (coupon.isUserSpecific()) {
            if (user == null) {
                return null;
            }
            Optional<UserCoupon> uc = userCouponRepository.findByUserAndCoupon(user, coupon);
            if (uc.isEmpty() || uc.get().isUsed()) {
                return null;
            }
        } else {
            // Public coupon: check if user already used it
            if (user != null && userCouponRepository.existsByUserAndCouponAndUsedTrue(user, coupon)) {
                return null;
            }
        }

        return coupon;
    }

    // =====================================================
    // APPLY COUPON — pessimistic lock + increment usageCount.
    // MUST be called inside the same @Transactional as checkout.
    //
    // Throws IllegalStateException if coupon is invalid at
    // apply-time instead of silently returning full price.
    // =====================================================
    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal orderTotal, User user) {
        if (code == null || code.isBlank()) {
            return orderTotal;
        }

        // PESSIMISTIC LOCK — blocks concurrent transactions until this one commits
        Coupon coupon = couponRepository
                .findByCodeForUpdate(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalStateException(
                "Coupon '" + code.trim().toUpperCase() + "' is not valid."));

        if (!coupon.isValid(orderTotal)) {
            log.warn("Coupon invalid at apply time | code={} | orderTotal={}", code, orderTotal);
            throw new IllegalStateException(
                    "Coupon '" + code.trim().toUpperCase()
                    + "' has expired or does not meet order requirements. "
                    + "Please choose another code.");
        }

        // User-specific coupon: verify ownership and not already used
        if (coupon.isUserSpecific()) {
            if (user == null) {
                throw new IllegalStateException("You must be logged in to use this coupon.");
            }
            UserCoupon uc = userCouponRepository.findByUserAndCoupon(user, coupon)
                    .orElseThrow(() -> new IllegalStateException(
                    "This coupon does not belong to you."));
            if (uc.isUsed()) {
                throw new IllegalStateException("You have already used this coupon.");
            }
            // Mark as used
            uc.setUsed(true);
            uc.setUsedAt(LocalDateTime.now());
            userCouponRepository.save(uc);
        } else {
            // Public coupon: track usage per user
            if (user != null) {
                if (userCouponRepository.existsByUserAndCouponAndUsedTrue(user, coupon)) {
                    throw new IllegalStateException("You have already used this coupon.");
                }
                // Create usage record
                Optional<UserCoupon> existingUc = userCouponRepository.findByUserAndCoupon(user, coupon);
                UserCoupon uc;
                if (existingUc.isPresent()) {
                    uc = existingUc.get();
                } else {
                    uc = new UserCoupon();
                    uc.setUser(user);
                    uc.setCoupon(coupon);
                }
                uc.setUsed(true);
                uc.setUsedAt(LocalDateTime.now());
                userCouponRepository.save(uc);
            }
        }

        BigDecimal discounted = coupon.applyDiscount(orderTotal);

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        log.info("Coupon applied | code={} | original={} | discounted={} | user={}",
                code, orderTotal, discounted, user != null ? user.getEmail() : "GUEST");
        return discounted;
    }

    /**
     * Backward-compatible overload without user context.
     */
    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal orderTotal) {
        return applyCoupon(code, orderTotal, null);
    }

    // =====================================================
    // DISPLAY DTO — used for UI rendering
    // =====================================================
    @lombok.Data
    public static class CouponDisplayDTO {
        private String code;
        private String description;
        private Coupon.DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal minOrderAmount;
        private LocalDateTime startDate;
        private LocalDateTime expiryDate;
        private boolean used;
        private boolean expired;
        private boolean active;
        private boolean usageLimitReached;
        private boolean userSpecific;

        /**
         * Calculate approximate discount amount for sorting.
         */
        public BigDecimal calculateDiscountAmount(BigDecimal orderTotal) {
            if (discountType == Coupon.DiscountType.PERCENTAGE) {
                return orderTotal.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            } else {
                return discountValue.min(orderTotal);
            }
        }

        /**
         * Format discount value for display.
         */
        public String getDiscountDisplay() {
            if (discountType == Coupon.DiscountType.PERCENTAGE) {
                return discountValue.stripTrailingZeros().toPlainString() + "%";
            } else {
                return new java.text.DecimalFormat("#,###").format(discountValue) + "₫";
            }
        }

        /**
         * Check if coupon has started (for display purposes).
         */
        public boolean isStarted() {
            return startDate == null || !startDate.isAfter(LocalDateTime.now());
        }

        /**
         * Check if coupon is currently usable.
         */
        public boolean isUsable() {
            return !used && !expired && active && !usageLimitReached
                    && (startDate == null || !startDate.isAfter(LocalDateTime.now()));
        }
    }
}
