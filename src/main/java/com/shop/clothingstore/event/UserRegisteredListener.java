package com.shop.clothingstore.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.service.CouponService;

/**
 * Listens for UserRegisteredEvent and creates a welcome coupon
 * for newly registered users.
 */
@Component
public class UserRegisteredListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);

    private final CouponService couponService;

    public UserRegisteredListener(CouponService couponService) {
        this.couponService = couponService;
    }

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            couponService.createWelcomeCouponForUser(event.getUser());
            log.info("Welcome coupon created for user: {}", event.getUser().getEmail());
        } catch (Exception e) {
            // Don't fail registration if coupon creation fails
            log.error("Failed to create welcome coupon for user: {}", event.getUser().getEmail(), e);
        }
    }
}
