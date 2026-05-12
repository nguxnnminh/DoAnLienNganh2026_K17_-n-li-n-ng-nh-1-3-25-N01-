package com.shop.clothingstore.service;

import com.shop.clothingstore.dto.DashboardReportDTO;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardReportDTO getFullReport() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startWeek = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startYear = today.withDayOfYear(1).atStartOfDay();

        long totalOrders = orderRepository.count();
        long ordersToday = orderRepository.countByCreatedAtBetween(startToday, startTomorrow);
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long processing = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shipping = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long cancelRequested = orderRepository.countByStatus(OrderStatus.CANCEL_REQUESTED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        double revenue = asDouble(orderRepository.getTotalAmountByStatus(OrderStatus.COMPLETED));
        double revenueToday = asDouble(orderRepository.getTotalAmountByStatusBetween(
                OrderStatus.COMPLETED, startToday, startTomorrow));
        double revenueThisWeek = asDouble(orderRepository.getTotalAmountByStatusBetween(
                OrderStatus.COMPLETED, startWeek, startTomorrow));
        double revenueThisMonth = asDouble(orderRepository.getTotalAmountByStatusBetween(
                OrderStatus.COMPLETED, startMonth, startTomorrow));
        double revenueThisYear = asDouble(orderRepository.getTotalAmountByStatusBetween(
                OrderStatus.COMPLETED, startYear, startTomorrow));
        double averageOrderValue = asDouble(orderRepository.getAvgOrderValue(OrderStatus.COMPLETED));
        long totalUsers = userRepository.count();

        double completionRate = totalOrders > 0 ? (completed * 100.0 / totalOrders) : 0.0;

        return new DashboardReportDTO(
                totalOrders,
                ordersToday,
                pending,
                processing,
                shipping,
                completed,
                cancelRequested,
                cancelled,
                revenue,
                revenueToday,
                revenueThisWeek,
                revenueThisMonth,
                revenueThisYear,
                averageOrderValue,
                totalUsers,
                completionRate);
    }

    public byte[] exportDashboardToExcel() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dashboard Report");

        // Header
        Row header = sheet.createRow(0);
        String[] columns = {"Metric", "Value"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        DashboardReportDTO report = getFullReport();
        int rowNum = 1;

        rowNum = addRow(sheet, rowNum, "Total Orders", report.getTotalOrders());
        rowNum = addRow(sheet, rowNum, "Orders Today", report.getOrdersToday());
        rowNum = addRow(sheet, rowNum, "Pending Orders", report.getPendingOrders());
        rowNum = addRow(sheet, rowNum, "Processing Orders", report.getProcessingOrders());
        rowNum = addRow(sheet, rowNum, "Shipping Orders", report.getShippingOrders());
        rowNum = addRow(sheet, rowNum, "Completed Orders", report.getCompletedOrders());
        rowNum = addRow(sheet, rowNum, "Cancel Requested Orders", report.getCancelRequestedOrders());
        rowNum = addRow(sheet, rowNum, "Cancelled Orders", report.getCancelledOrders());
        rowNum = addRow(sheet, rowNum, "Total Revenue (VND)", report.getTotalRevenue());
        rowNum = addRow(sheet, rowNum, "Revenue Today (VND)", report.getRevenueToday());
        rowNum = addRow(sheet, rowNum, "Revenue This Week (VND)", report.getRevenueThisWeek());
        rowNum = addRow(sheet, rowNum, "Revenue This Month (VND)", report.getRevenueThisMonth());
        rowNum = addRow(sheet, rowNum, "Revenue This Year (VND)", report.getRevenueThisYear());
        rowNum = addRow(sheet, rowNum, "Average Order Value (VND)", report.getAverageOrderValue());
        rowNum = addRow(sheet, rowNum, "Total Customers", report.getTotalCustomers());
        rowNum = addRow(sheet, rowNum, "Completion Rate (%)", String.format("%.2f", report.getCompletionRate()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private int addRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value.toString());
        return rowNum + 1;
    }

    private double asDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
