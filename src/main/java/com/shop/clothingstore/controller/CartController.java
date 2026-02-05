package com.shop.clothingstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.clothingstore.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String cart(Model model) {
        model.addAttribute("cartItems", cartService.getCart());
        model.addAttribute("total", cartService.getTotal());
        return "shop/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam("variantId") Long variantId) {
        cartService.addToCart(variantId);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity
    ) {
        cartService.updateQuantity(variantId, quantity);
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
