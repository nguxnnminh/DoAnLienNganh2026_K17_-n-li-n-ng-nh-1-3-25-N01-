package com.shop.clothingstore.controller.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.ProductResponse;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.RecommendationService;
import com.shop.clothingstore.service.UserService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationApiController(
            RecommendationService recommendationService,
            UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    // GET /api/recommendations?limit=8
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            Principal principal) {

        User user = getUser(principal);

        List<ProductResponse> result = recommendationService
                .getRecommendations(user, limit).stream()
                .map(ProductResponse::summary)
                .toList();

        return ResponseEntity.ok(result);
    }

    // GET /api/products/{id}/similar?limit=6
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<ProductResponse>> getSimilar(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {

        List<ProductResponse> result = recommendationService
                .getSimilarProducts(productId, limit).stream()
                .map(ProductResponse::summary)
                .toList();

        return ResponseEntity.ok(result);
    }

    private User getUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }
}
