package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.repository.base.BaseRepository;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends BaseRepository<Coupon, Long> {

    // Read-only — for validation UI (no lock needed)
    Optional<Coupon> findByCodeAndActiveTrue(String code);

    // All active public (non-user-specific) coupons
    List<Coupon> findByActiveTrueAndUserSpecificFalse();

    boolean existsByCodeIgnoreCase(String code);

    // Pessimistic write lock — use inside @Transactional when applying discount.
    // Prevents two concurrent checkouts from both reading usageCount=N
    // and both writing usageCount=N+1 (lost-update race condition).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.active = true")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}
