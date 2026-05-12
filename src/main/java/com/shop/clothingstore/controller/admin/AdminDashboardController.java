package com.shop.clothingstore.controller.admin;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.shop.clothingstore.dto.DashboardReportDTO;
import com.shop.clothingstore.service.DashboardService;
import com.shop.clothingstore.service.ReportService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController extends AdminBaseController {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    @GetMapping("/admin")
    public String dashboard(Model model) {

        model.addAttribute("title", "Dashboard");

        var dashboard = dashboardService.getDashboardData();

        model.addAttribute("totalTransactions", dashboard.getTotalTransactions());
        model.addAttribute("ordersToday", dashboard.getOrdersToday());
        model.addAttribute("pendingTransactions", dashboard.getPendingTransactions());
        model.addAttribute("processingTransactions", dashboard.getProcessingTransactions());
        model.addAttribute("shippingTransactions", dashboard.getShippingTransactions());
        model.addAttribute("completedTransactions", dashboard.getCompletedTransactions());
        model.addAttribute("cancelledTransactions", dashboard.getCancelledTransactions());
        model.addAttribute("cancelRequestedTransactions", dashboard.getCancelRequestedTransactions());
        model.addAttribute("totalAmount", dashboard.getTotalAmount());
        model.addAttribute("revenueToday", dashboard.getRevenueToday());
        model.addAttribute("revenueThisWeek", dashboard.getRevenueThisWeek());
        model.addAttribute("revenueThisMonth", dashboard.getRevenueThisMonth());
        model.addAttribute("revenueThisYear", dashboard.getRevenueThisYear());
        model.addAttribute("avgOrderValue", dashboard.getAvgOrderValue());
        model.addAttribute("totalUsers", dashboard.getTotalUsers());
        model.addAttribute("totalStock", dashboard.getTotalStock());
        model.addAttribute("totalSold", dashboard.getTotalSold());
        model.addAttribute("latestTransactions", dashboard.getLatestTransactions());
        model.addAttribute("amountLabels", dashboard.getAmountLabels());
        model.addAttribute("amountData", dashboard.getAmountData());
        model.addAttribute("dailyRevenueLabels", dashboard.getDailyRevenueLabels());
        model.addAttribute("dailyRevenueData", dashboard.getDailyRevenueData());
        model.addAttribute("weeklyRevenueLabels", dashboard.getWeeklyRevenueLabels());
        model.addAttribute("weeklyRevenueData", dashboard.getWeeklyRevenueData());
        model.addAttribute("monthlyRevenueLabels", dashboard.getMonthlyRevenueLabels());
        model.addAttribute("monthlyRevenueData", dashboard.getMonthlyRevenueData());
        model.addAttribute("yearlyRevenueLabels", dashboard.getYearlyRevenueLabels());
        model.addAttribute("yearlyRevenueData", dashboard.getYearlyRevenueData());

        // New BI report data
        DashboardReportDTO report = dashboardService.getFullReport();
        model.addAttribute("report", report);

        // Low stock alert
        model.addAttribute("lowStockCount", dashboardService.getLowStockCount());
        model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts());

        // Extended analytics
        model.addAttribute("topProducts", dashboardService.getTopSellingProducts());

        return "admin/dashboard";
    }

    // Export Excel
    @PostMapping("/admin/export-excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        byte[] excelBytes = reportService.exportDashboardToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "dashboard-report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
