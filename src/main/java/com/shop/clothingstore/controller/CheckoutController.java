package com.shop.clothingstore.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.CheckoutService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final UserRepository userRepository;

    public CheckoutController(
            CheckoutService checkoutService,
            CartService cartService,
            UserRepository userRepository
    ) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    // ===============================
    // GET CHECKOUT PAGE (AUTOFILL + CART)
    // ===============================
    @GetMapping("/checkout")
    public String checkoutPage(
            Model model,
            Principal principal
    ) {

        // ===== CART DATA =====
        model.addAttribute("cartItems", cartService.getCart());
        model.addAttribute("total", cartService.getTotal());

        // ===== AUTOFILL USER =====
        if (principal != null) {
            User user = userRepository
                    .findByEmail(principal.getName())
                    .orElse(null);
            model.addAttribute("user", user);
        }

        return "shop/checkout";
    }

    // ===============================
    // POST CHECKOUT (STOCK + ORDER)
    // ===============================
    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam String customerName,
            @RequestParam String phone,
            @RequestParam String address,
            HttpSession session,
            Principal principal,
            Model model
    ) {

        User user = null;
        if (principal != null) {
            user = userRepository
                    .findByEmail(principal.getName())
                    .orElse(null);
        }

        try {
            var order = checkoutService.checkout(
                    customerName,
                    phone,
                    address,
                    session,
                    user
            );

            model.addAttribute("order", order);
            return "shop/checkout-success";

        } catch (IllegalStateException e) {

            // ===== ERROR =====
            model.addAttribute("error", e.getMessage());

            // ===== CART DATA (BẮT BUỘC LOAD LẠI) =====
            model.addAttribute("cartItems", cartService.getCart());
            model.addAttribute("total", cartService.getTotal());

            // ===== AUTOFILL USER =====
            if (user != null) {
                model.addAttribute("user", user);
            }

            return "shop/checkout";
        }
    }
}
