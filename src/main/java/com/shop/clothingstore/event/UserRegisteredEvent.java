package com.shop.clothingstore.event;

import com.shop.clothingstore.entity.User;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a new user completes registration.
 * Listeners can react to this event without creating circular dependencies
 * (e.g., CouponService creating a welcome coupon).
 */
public class UserRegisteredEvent extends ApplicationEvent {

    private final User user;

    public UserRegisteredEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
