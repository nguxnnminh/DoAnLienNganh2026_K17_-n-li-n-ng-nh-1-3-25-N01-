package com.shop.clothingstore.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.CheckoutService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final int MAX_QUANTITY = 99;

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String cart(Model model) {
        BigDecimal subtotal = cartService.getTotal();
        BigDecimal shippingFee = subtotal.compareTo(CheckoutService.FREE_SHIP_THRESHOLD) >= 0
                ? BigDecimal.ZERO : CheckoutService.SHIP_FEE;
        model.addAttribute("cartItems", cartService.getCart());
        model.addAttribute("total", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", subtotal.add(shippingFee));
        return "shop/cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long variantId,
            @RequestParam int quantity,
            RedirectAttributes redirectAttributes
    ) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            redirectAttributes.addFlashAttribute("error",
                    "Quantity must be between 1 and " + MAX_QUANTITY);
            return "redirect:/cart";
        }

        try {
            cartService.addToCart(variantId, quantity);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity,
            RedirectAttributes redirectAttributes
    ) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            redirectAttributes.addFlashAttribute("error",
                    "Quantity must be between 1 and " + MAX_QUANTITY);
            return "redirect:/cart";
        }

        try {
            cartService.updateQuantity(variantId, quantity);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam("variantId") Long variantId) {
        cartService.remove(variantId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear() {
        cartService.clear();
        return "redirect:/cart";
    }
}
