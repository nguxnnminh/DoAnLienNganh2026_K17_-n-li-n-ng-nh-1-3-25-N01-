package com.shop.clothingstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.ReviewService;
import com.shop.clothingstore.service.UserService;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService,
            UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @PostMapping("/reviews/{orderItemId}")
    public String createReview(
            @PathVariable Long orderItemId,
            @org.springframework.web.bind.annotation.RequestParam double rating,
            @org.springframework.web.bind.annotation.RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService
                .findByEmail(authentication.getName())
                .orElseThrow();

        try {

            Long orderId = reviewService.createReview(
                    user.getId(),
                    orderItemId,
                    rating,
                    comment
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đánh giá thành công!"
            );

            return "redirect:/orders/" + orderId;

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/orders/" + orderItemId;
        }
    }
}
