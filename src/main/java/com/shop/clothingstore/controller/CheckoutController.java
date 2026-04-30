package com.shop.clothingstore.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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

    // Vietnam phone: 10 digits starting with 0, or +84 prefix
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+84|0)(3|5|7|8|9)\\d{8}$");

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
            CouponService couponService) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.userService = userService;
        this.orderService = orderService;
        this.couponService = couponService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        loadCartData(model);
        User user = getCurrentUser(principal);
        if (user != null) {
            model.addAttribute("user", user);
            // Load available coupons for this user and order total
            BigDecimal total = cartService.getTotal();
            List<CouponService.CouponDisplayDTO> availableCoupons =
                    couponService.getAvailableCouponsForUser(user, total);
            model.addAttribute("availableCoupons", availableCoupons);
        } else {
            model.addAttribute("availableCoupons", Collections.emptyList());
        }
        model.addAttribute("isLoggedIn", user != null);
        return "shop/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam String customerName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) String note,
            HttpSession session,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        // Server-side validation — HTML 'required' alone is not sufficient
        if (customerName == null || customerName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Ho ten khong duoc de trong.");
            return "redirect:/checkout";
        }
        if (customerName.length() > 100) {
            redirectAttributes.addFlashAttribute("error", "Ho ten khong duoc vuot qua 100 ky tu.");
            return "redirect:/checkout";
        }
        if (phone == null || phone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "So dien thoai khong duoc de trong.");
            return "redirect:/checkout";
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            redirectAttributes.addFlashAttribute("error",
                    "So dien thoai khong hop le. Vui long nhap so dien thoai Viet Nam (VD: 0901234567).");
            return "redirect:/checkout";
        }
        if (address == null || address.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Dia chi giao hang khong duoc de trong.");
            return "redirect:/checkout";
        }
        if (address.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "Dia chi khong duoc vuot qua 500 ky tu.");
            return "redirect:/checkout";
        }

        User user = getCurrentUser(principal);

        try {
            Order order = checkoutService.checkout(
                    customerName.trim(),
                    phone.trim(),
                    address.trim(),
                    cartService.getCart(),
                    user,
                    couponCode,
                    note
            );
            cartService.clear();
            redirectAttributes.addFlashAttribute("orderId", order.getId());
            return "redirect:/checkout/success";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

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
