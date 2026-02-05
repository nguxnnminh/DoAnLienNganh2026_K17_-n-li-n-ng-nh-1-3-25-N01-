package com.shop.clothingstore.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminDashboardController(
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin")
    public String dashboard(Model model, HttpServletRequest request) {

        // ===== STATISTICS =====
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);

        BigDecimal totalRevenue =
                orderRepository.getTotalRevenueByStatus(OrderStatus.COMPLETED);

        long totalUsers = userRepository.count();

        // ===== LATEST ORDERS =====
        List<Order> latestOrders =
                orderRepository.findTop5ByOrderByCreatedAtDesc();

        // ===== ADD MODEL =====
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("latestOrders", latestOrders);

        // ⭐ ACTIVE MENU
        model.addAttribute("currentUri", request.getRequestURI());

        // ⭐ PAGE TITLE
        model.addAttribute("title", "Dashboard");

        // ⭐ DEMO CHART DATA
        model.addAttribute("revenueLabels",
                List.of("T2","T3","T4","T5","T6","T7","CN"));

        model.addAttribute("revenueData",
                List.of(1200000,1500000,800000,2000000,2500000,1800000,2200000));

        return "admin/dashboard";
    }

}
