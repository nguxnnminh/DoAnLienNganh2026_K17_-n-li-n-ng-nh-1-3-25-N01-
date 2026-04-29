package com.shop.clothingstore.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.WishlistResponse;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;
import com.shop.clothingstore.service.WishlistService;

/**
 * REST API cho Wishlist. FIXED: Trả DTO thay vì entity trực tiếp — tránh
 * circular reference và lộ data nội bộ.
 */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistApiController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistApiController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(Authentication authentication) {
        User user = getUser(authentication);
        List<WishlistResponse> response = wishlistService.getWishlist(user).stream()
                .map(WishlistResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        User user = getUser(authentication);
        wishlistService.addToWishlist(user, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        User user = getUser(authentication);
        wishlistService.removeFromWishlist(user, productId);
        return ResponseEntity.ok().build();
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Unauthorized");
        }
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
