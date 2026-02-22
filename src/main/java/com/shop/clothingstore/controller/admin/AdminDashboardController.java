package com.shop.clothingstore.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.UserRepository;

@Controller
public class AdminDashboardController extends AdminBaseController {

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
    public String dashboard(Model model) {

        // ================= TITLE =================
        model.addAttribute("title", "Dashboard");


        // ================= KPI =================
        long totalOrders = orderRepository.count();

        long pendingOrders =
                orderRepository.countByStatus(OrderStatus.PENDING);

        long completedOrders =
                orderRepository.countByStatus(OrderStatus.COMPLETED);

        BigDecimal totalRevenue =
                orderRepository.getTotalRevenueByStatus(OrderStatus.COMPLETED);

        long totalUsers = userRepository.count();


        // ================= REVENUE CHART =================
        List<Object[]> revenueRaw =
                orderRepository.getRevenueByDate(OrderStatus.COMPLETED);

        List<String> revenueLabels = new ArrayList<>();
        List<BigDecimal> revenueData = new ArrayList<>();

        for (Object[] row : revenueRaw) {

            if (row == null || row[0] == null || row[1] == null) continue;

            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate localDate = sqlDate.toLocalDate();

            Number revenueNumber = (Number) row[1];

            BigDecimal revenue =
                    BigDecimal.valueOf(revenueNumber.doubleValue());

            revenueLabels.add(localDate.toString());
            revenueData.add(revenue);
        }


        // ================= LATEST ORDERS =================
        List<Order> latestOrders =
                orderRepository.findTop5ByOrderByCreatedAtDesc();


        // ================= MODEL =================
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalRevenue",
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("latestOrders", latestOrders);
        model.addAttribute("revenueLabels", revenueLabels);
        model.addAttribute("revenueData", revenueData);

        return "admin/dashboard";
    }
}