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

        // Legacy data (charts + KPI)
        var dashboard = dashboardService.getDashboardData();

        model.addAttribute("totalTransactions", dashboard.getTotalTransactions());
        model.addAttribute("pendingTransactions", dashboard.getPendingTransactions());
        model.addAttribute("completedTransactions", dashboard.getCompletedTransactions());
        model.addAttribute("totalAmount", dashboard.getTotalAmount());
        model.addAttribute("totalUsers", dashboard.getTotalUsers());
        model.addAttribute("latestTransactions", dashboard.getLatestTransactions());
        model.addAttribute("amountLabels", dashboard.getAmountLabels());
        model.addAttribute("amountData", dashboard.getAmountData());

        // New BI report data
        DashboardReportDTO report = dashboardService.getFullReport();
        model.addAttribute("report", report);

        // Low stock alert
        model.addAttribute("lowStockCount", dashboardService.getLowStockCount());
        model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts());

        // Extended analytics
        model.addAttribute("topProducts", dashboardService.getTopSellingProducts());
        model.addAttribute("avgOrderValue", dashboardService.getAvgOrderValue());
        model.addAttribute("processingTransactions", dashboardService.getProcessingCount());
        model.addAttribute("shippingTransactions", dashboardService.getShippingCount());

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
