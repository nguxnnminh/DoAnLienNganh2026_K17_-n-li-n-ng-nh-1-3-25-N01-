package com.shop.clothingstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.ReviewService;
import com.shop.clothingstore.service.UserService;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @PostMapping("/reviews/{orderItemId}")
    public String createReview(
            @PathVariable Long orderItemId,
            @RequestParam double rating,
            @RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        try {
            // createReview returns the ORDER id (not item id) — use it for the redirect
            Long orderId = reviewService.createReview(user.getId(), orderItemId, rating, comment);

            redirectAttributes.addFlashAttribute("success", "Review submitted successfully!");
            return "redirect:/orders/" + orderId;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // BUG-04 FIX: we need the orderId for the redirect.
            // If createReview failed before returning it, fall back to /my-orders
            // since we cannot reliably know which order this item belongs to from here.
            return "redirect:/my-orders";
        }
    }
}
