package com.shop.clothingstore.controller.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.dto.api.CartRequest;
import com.shop.clothingstore.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private final CartService cartService;

    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    // =====================================================
    // GET /api/cart
    // =====================================================
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart() {

        List<CartItemDTO> items = cartService.getCart();
        BigDecimal total = cartService.getTotal();

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", total);
        response.put("itemCount", items.size());

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // POST /api/cart/add
    // =====================================================
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @Valid @RequestBody CartRequest request) {

        cartService.addToCart(request.getVariantId(), request.getQuantity());

        return getCart();
    }

    // =====================================================
    // PUT /api/cart/update
    // =====================================================
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateQuantity(
            @Valid @RequestBody CartRequest request) {

        cartService.updateQuantity(request.getVariantId(), request.getQuantity());

        return getCart();
    }

    // =====================================================
    // DELETE /api/cart/{variantId}
    // =====================================================
    @DeleteMapping("/{variantId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable Long variantId) {

        cartService.remove(variantId);

        return getCart();
    }

    // =====================================================
    // DELETE /api/cart
    // =====================================================
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCart() {

        cartService.clear();

        return getCart();
    }
}
