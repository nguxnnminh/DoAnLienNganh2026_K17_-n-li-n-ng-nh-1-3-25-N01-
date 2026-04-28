package com.shop.clothingstore.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.CheckoutService;
import com.shop.clothingstore.service.CouponService;
import com.shop.clothingstore.service.OrderService;
import com.shop.clothingstore.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;
    private final CouponService couponService;

    public CheckoutController(
            CheckoutService checkoutService,
            CartService cartService,
            UserService userService,
            OrderService orderService,
            CouponService couponService
    ) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.userService = userService;
        this.orderService = orderService;
        this.couponService = couponService;
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
    // POST CHECKOUT (PRG: Post → Redirect → Get)
    // FIX: thêm couponCode param, truyền vào CheckoutService
    // ===============================
    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam String customerName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam(required = false) String couponCode,
            HttpSession session,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User user = getCurrentUser(principal);

        try {
            var order = checkoutService.checkout(
                    customerName,
                    phone,
                    address,
                    cartService.getCart(),
                    user,
                    couponCode
            );

            cartService.clear();

            redirectAttributes.addFlashAttribute("orderId", order.getId());
            return "redirect:/checkout/success";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    // ===============================
    // GET CHECKOUT SUCCESS (PRG)
    // ===============================
    @GetMapping("/checkout/success")
    public String checkoutSuccess(Model model) {
        Long orderId = (Long) model.asMap().get("orderId");

        if (orderId == null) {
            return "redirect:/";
        }

        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        return "shop/checkout-success";
    }

    // ===============================
    // PRIVATE HELPERS
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
