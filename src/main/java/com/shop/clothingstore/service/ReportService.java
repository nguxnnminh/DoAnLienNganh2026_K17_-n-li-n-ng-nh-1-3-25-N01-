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

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardReportDTO getFullReport() {
        long totalOrders = orderRepository.count();
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        double revenue = orderRepository.getTotalAmountByStatus(OrderStatus.COMPLETED).doubleValue();
        long totalUsers = userRepository.count();

        double completionRate = totalOrders > 0 ? (completed * 100.0 / totalOrders) : 0.0;

        return new DashboardReportDTO(totalOrders, completed, cancelled, revenue, totalUsers, completionRate);
    }

    public byte[] exportDashboardToExcel() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Báo cáo Dashboard");

        // Header
        Row header = sheet.createRow(0);
        String[] columns = {"Chỉ số", "Giá trị"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        DashboardReportDTO report = getFullReport();
        int rowNum = 1;

        rowNum = addRow(sheet, rowNum, "Tổng số đơn hàng", report.getTotalOrders());
        rowNum = addRow(sheet, rowNum, "Đơn hàng hoàn thành", report.getCompletedOrders());
        rowNum = addRow(sheet, rowNum, "Đơn hàng bị hủy", report.getCancelledOrders());
        rowNum = addRow(sheet, rowNum, "Tổng doanh thu (VND)", report.getTotalRevenue());
        rowNum = addRow(sheet, rowNum, "Tổng số khách hàng", report.getTotalCustomers());
        rowNum = addRow(sheet, rowNum, "Tỷ lệ hoàn thành (%)", String.format("%.2f", report.getCompletionRate()));

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
}
