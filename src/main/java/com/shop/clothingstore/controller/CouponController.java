package com.shop.clothingstore.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.CouponService;
import com.shop.clothingstore.service.UserService;

@Controller
public class CouponController {

    private final CouponService couponService;
    private final UserService userService;

    public CouponController(CouponService couponService, UserService userService) {
        this.couponService = couponService;
        this.userService = userService;
    }

    @GetMapping("/my-coupons")
    public String myCoupons(
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(required = false, defaultValue = "best") String sort,
            Principal principal,
            Model model) {

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        List<CouponService.CouponDisplayDTO> allCoupons = couponService.getAllCouponsForUser(user);

        // Filter
        List<CouponService.CouponDisplayDTO> filtered = switch (filter) {
            case "available" -> allCoupons.stream().filter(CouponService.CouponDisplayDTO::isUsable).toList();
            case "used" -> allCoupons.stream().filter(CouponService.CouponDisplayDTO::isUsed).toList();
            case "expired" -> allCoupons.stream().filter(c -> c.isExpired() || c.isUsageLimitReached()).toList();
            default -> allCoupons;
        };

        // Sort
        filtered = switch (sort) {
            case "expiring" -> filtered.stream()
                    .sorted((a, b) -> {
                        if (a.getExpiryDate() == null && b.getExpiryDate() == null) return 0;
                        if (a.getExpiryDate() == null) return 1;
                        if (b.getExpiryDate() == null) return -1;
                        return a.getExpiryDate().compareTo(b.getExpiryDate());
                    })
                    .toList();
            default -> // "best" — highest discount first
                    filtered.stream()
                    .sorted((a, b) -> b.getDiscountValue().compareTo(a.getDiscountValue()))
                    .toList();
        };

        model.addAttribute("coupons", filtered);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("totalAvailable", allCoupons.stream()
                .filter(CouponService.CouponDisplayDTO::isUsable).count());

        return "shop/my-coupons";
    }
}
