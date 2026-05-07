package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
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

    @Cacheable("dashboardData")
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

    // ==================== BUSINESS DASHBOARD (NEW) ====================
    public DashboardReportDTO getFullReport() {
        return reportService.getFullReport();
    }

    public long getLowStockCount() {
        return variantRepository.countByStockLessThan(10);
    }

    public byte[] exportToExcel() throws Exception {
        return reportService.exportDashboardToExcel();
    }

    // Fetch low-stock product variants
    public List<ProductVariant> getLowStockProducts() {
        return variantRepository.findByStockLessThan(10);
    }

    public List<Map<String, Object>> getTopSellingProducts() {
        List<Object[]> raw = orderRepository.getTopSellingProducts(
                OrderStatus.COMPLETED, PageRequest.of(0, 5));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("revenue", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            item.put("qty", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            result.add(item);
        }
        return result;
    }

    public BigDecimal getAvgOrderValue() {
        BigDecimal avg = orderRepository.getAvgOrderValue(OrderStatus.COMPLETED);
        return avg != null ? avg : BigDecimal.ZERO;
    }

    public long getProcessingCount() {
        return orderRepository.countByStatus(OrderStatus.PROCESSING);
    }

    public long getShippingCount() {
        return orderRepository.countByStatus(OrderStatus.SHIPPING);
    }
}
