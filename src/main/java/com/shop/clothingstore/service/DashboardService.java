package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.dto.DashboardDTO;
import com.shop.clothingstore.dto.DashboardReportDTO;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.repository.UserRepository;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final ReportService reportService;

    public DashboardService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductVariantRepository variantRepository,
            ReportService reportService) {

        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
        this.reportService = reportService;
    }

    public DashboardDTO getDashboardData() {

        long totalTransactions = orderRepository.count();

        long pendingTransactions = orderRepository.countByStatus(OrderStatus.PENDING);

        long completedTransactions = orderRepository.countByStatus(OrderStatus.COMPLETED);

        BigDecimal totalAmount = orderRepository.getTotalAmountByStatus(OrderStatus.COMPLETED);

        long totalUsers = userRepository.count();

        // ===== CHART =====
        List<Object[]> raw = orderRepository.getTotalAmountByDate(OrderStatus.COMPLETED);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        for (Object[] row : raw) {
            if (row == null || row[0] == null || row[1] == null) {
                continue;
            }

            java.sql.Date sqlDate = (java.sql.Date) row[0];
            java.time.LocalDate localDate = sqlDate.toLocalDate();

            Number amountNumber = (Number) row[1];

            labels.add(localDate.toString());
            data.add(BigDecimal.valueOf(amountNumber.doubleValue()));
        }

        List<Order> latestTransactions = orderRepository.findTop5ByOrderByCreatedAtDesc();

        return new DashboardDTO(
                totalTransactions,
                pendingTransactions,
                completedTransactions,
                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                totalUsers,
                latestTransactions,
                labels,
                data
        );
    }

    // ==================== PHẦN MỚI CHO DASHBOARD KINH DOANH ====================
    public DashboardReportDTO getFullReport() {
        return reportService.getFullReport();
    }

    public long getLowStockCount() {
        return variantRepository.countByStockLessThan(10);
    }

    public byte[] exportToExcel() throws Exception {
        return reportService.exportDashboardToExcel();
    }

    // Lấy danh sách sản phẩm tồn kho thấp
    public List<ProductVariant> getLowStockProducts() {
        return variantRepository.findByStockLessThan(10);
    }
}
