package com.shop.clothingstore.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.CheckoutService;
import com.shop.clothingstore.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final UserService userService; // ✅ dùng service thay repository

    public CheckoutController(
            CheckoutService checkoutService,
            CartService cartService,
            UserService userService
    ) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.userService = userService;
    }

    // ===============================
    // GET CHECKOUT PAGE
    // ===============================
    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {

        loadCartData(model);

        User user = getCurrentUser(principal);
        if (user != null) {
            model.addAttribute("user", user);
        }

        return "shop/checkout";
    }

    // ===============================
    // POST CHECKOUT
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

        User user = getCurrentUser(principal);

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

            model.addAttribute("error", e.getMessage());
            loadCartData(model);

            if (user != null) {
                model.addAttribute("user", user);
            }

            return "shop/checkout";
        }
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================
    private void loadCartData(Model model) {
        model.addAttribute("cartItems", cartService.getCart());
        model.addAttribute("total", cartService.getTotal());
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }
}
