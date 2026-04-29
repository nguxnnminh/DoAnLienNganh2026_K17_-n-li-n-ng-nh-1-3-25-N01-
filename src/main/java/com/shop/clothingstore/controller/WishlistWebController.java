package com.shop.clothingstore.controller;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;
import com.shop.clothingstore.service.WishlistService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/wishlist")
public class WishlistWebController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistWebController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @PostMapping("/{productId}/add")
    public String add(@PathVariable Long productId,
                      Authentication authentication,
                      HttpServletRequest request) {
        wishlistService.addToWishlist(resolveUser(authentication), Objects.requireNonNull(productId));
        return redirectBack(request);
    }

    @PostMapping("/{productId}/remove")
    public String remove(@PathVariable Long productId,
                         Authentication authentication,
                         HttpServletRequest request) {
        wishlistService.removeFromWishlist(resolveUser(authentication), Objects.requireNonNull(productId));
        return redirectBack(request);
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String origin = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        if (referer != null && referer.startsWith(origin)) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
