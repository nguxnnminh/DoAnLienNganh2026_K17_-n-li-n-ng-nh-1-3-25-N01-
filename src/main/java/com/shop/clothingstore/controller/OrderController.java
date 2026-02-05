package com.shop.clothingstore.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.UserRepository;

@Controller
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ===============================
    // MY ORDERS (LIST)
    // ===============================
    @GetMapping("/my-orders")
    public String myOrders(
            Principal principal,
            Model model
    ) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        List<Order> orders = orderRepository
                .findByUserOrderByCreatedAtDesc(user);

        model.addAttribute("orders", orders);

        return "shop/my-orders";
    }

    // ===============================
    // ORDER DETAIL
    // ===============================
    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        Order order = orderRepository
                .findById(id)
                .orElseThrow();

        // 🔐 bảo mật: chỉ xem đơn của mình
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            return "redirect:/my-orders";
        }

        model.addAttribute("order", order);

        return "shop/order-detail";
    }
}
