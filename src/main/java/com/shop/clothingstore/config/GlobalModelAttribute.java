package com.shop.clothingstore.config;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalModelAttribute {

    private final CartService cartService;

    public GlobalModelAttribute(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("cartTotalQty")
    public int cartTotalQty() {
        List<CartItemDTO> cart = cartService.getCart();
        return cart.stream().mapToInt(CartItemDTO::getQuantity).sum();
    }

}
