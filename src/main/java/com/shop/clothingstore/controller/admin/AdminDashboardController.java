package com.shop.clothingstore.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.shop.clothingstore.service.DashboardService;

@Controller
public class AdminDashboardController extends AdminBaseController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {

        model.addAttribute("title", "Dashboard");

        // ===== LOAD DASHBOARD DATA FROM SERVICE =====
        var dashboard = dashboardService.getDashboardData();

        model.addAttribute("totalTransactions", dashboard.getTotalTransactions());
        model.addAttribute("pendingTransactions", dashboard.getPendingTransactions());
        model.addAttribute("completedTransactions", dashboard.getCompletedTransactions());
        model.addAttribute("totalAmount", dashboard.getTotalAmount());
        model.addAttribute("totalUsers", dashboard.getTotalUsers());
        model.addAttribute("latestTransactions", dashboard.getLatestTransactions());
        model.addAttribute("amountLabels", dashboard.getAmountLabels());
        model.addAttribute("amountData", dashboard.getAmountData());

        return "admin/dashboard";
    }
}
