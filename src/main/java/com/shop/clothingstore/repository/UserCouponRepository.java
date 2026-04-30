package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.entity.UserCoupon;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface UserCouponRepository extends BaseRepository<UserCoupon, Long> {

    List<UserCoupon> findByUser(User user);

    List<UserCoupon> findByUserAndUsedFalse(User user);

    Optional<UserCoupon> findByUserAndCoupon(User user, Coupon coupon);

    boolean existsByUserAndCouponAndUsedTrue(User user, Coupon coupon);

    boolean existsByUserAndCoupon(User user, Coupon coupon);
}
