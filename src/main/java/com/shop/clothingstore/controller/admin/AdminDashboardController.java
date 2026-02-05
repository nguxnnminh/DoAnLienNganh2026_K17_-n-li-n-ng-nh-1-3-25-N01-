package com.shop.clothingstore.controller.admin;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);

        BigDecimal totalRevenue =
                orderRepository.getTotalRevenueByStatus(OrderStatus.COMPLETED);

        long totalUsers = userRepository.count();

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalUsers", totalUsers);

        // ⭐ FIX URI ACTIVE MENU
        model.addAttribute("currentUri", request.getRequestURI());

        // ⭐ PAGE TITLE
        model.addAttribute("title", "Dashboard");

        return "admin/dashboard";
    }

}
