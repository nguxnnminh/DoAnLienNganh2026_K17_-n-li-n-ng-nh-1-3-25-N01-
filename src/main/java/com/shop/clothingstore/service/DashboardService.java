package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");

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
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startWeek = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startYear = today.withDayOfYear(1).atStartOfDay();

        long totalTransactions = orderRepository.count();
        long ordersToday = orderRepository.countByCreatedAtBetween(startToday, startTomorrow);
        long pendingTransactions = orderRepository.countByStatus(OrderStatus.PENDING);
        long processingTransactions = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shippingTransactions = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long completedTransactions = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long cancelledTransactions = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long cancelRequestedTransactions = orderRepository.countByStatus(OrderStatus.CANCEL_REQUESTED);

        BigDecimal totalAmount = safe(orderRepository.getTotalAmountByStatus(OrderStatus.COMPLETED));
        BigDecimal revenueToday = completedRevenueBetween(startToday, startTomorrow);
        BigDecimal revenueThisWeek = completedRevenueBetween(startWeek, startTomorrow);
        BigDecimal revenueThisMonth = completedRevenueBetween(startMonth, startTomorrow);
        BigDecimal revenueThisYear = completedRevenueBetween(startYear, startTomorrow);
        BigDecimal avgOrderValue = safe(orderRepository.getAvgOrderValue(OrderStatus.COMPLETED));

        RevenueSeries daily = buildDailyRevenue(today);
        RevenueSeries weekly = buildWeeklyRevenue(today);
        RevenueSeries monthly = buildMonthlyRevenue(today);
        RevenueSeries yearly = buildYearlyRevenue(today);

        List<Order> latestTransactions = orderRepository.findTop5ByOrderByCreatedAtDesc();

        return new DashboardDTO(
                totalTransactions,
                ordersToday,
                pendingTransactions,
                processingTransactions,
                shippingTransactions,
                completedTransactions,
                cancelledTransactions,
                cancelRequestedTransactions,
                totalAmount,
                revenueToday,
                revenueThisWeek,
                revenueThisMonth,
                revenueThisYear,
                avgOrderValue,
                userRepository.count(),
                safeLong(variantRepository.getTotalStock()),
                safeLong(variantRepository.getTotalSold()),
                latestTransactions,
                daily.labels(),
                daily.data(),
                weekly.labels(),
                weekly.data(),
                monthly.labels(),
                monthly.data(),
                yearly.labels(),
                yearly.data()
        );
    }

    public DashboardReportDTO getFullReport() {
        return reportService.getFullReport();
    }

    public long getLowStockCount() {
        return variantRepository.countByStockLessThan(LOW_STOCK_THRESHOLD);
    }

    public byte[] exportToExcel() throws Exception {
        return reportService.exportDashboardToExcel();
    }

    public List<ProductVariant> getLowStockProducts() {
        return variantRepository.findByStockLessThan(LOW_STOCK_THRESHOLD);
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

    private BigDecimal completedRevenueBetween(LocalDateTime start, LocalDateTime end) {
        return safe(orderRepository.getTotalAmountByStatusBetween(OrderStatus.COMPLETED, start, end));
    }

    private RevenueSeries buildDailyRevenue(LocalDate today) {
        LocalDate start = today.minusDays(29);
        LocalDate end = today.plusDays(1);
        LinkedHashMap<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            totals.put(start.plusDays(i), BigDecimal.ZERO);
        }

        for (Object[] row : orderRepository.getRevenueRowsBetween(
                OrderStatus.COMPLETED, start.atStartOfDay(), end.atStartOfDay())) {
            LocalDate day = toDate(row[0]);
            if (totals.containsKey(day)) {
                totals.put(day, totals.get(day).add(toBigDecimal(row[1])));
            }
        }

        return new RevenueSeries(
                totals.keySet().stream().map(d -> d.format(DAY_LABEL)).toList(),
                new ArrayList<>(totals.values()));
    }

    private RevenueSeries buildWeeklyRevenue(LocalDate today) {
        LocalDate currentWeek = today.with(DayOfWeek.MONDAY);
        LocalDate start = currentWeek.minusWeeks(11);
        LocalDate end = today.plusDays(1);
        LinkedHashMap<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            totals.put(start.plusWeeks(i), BigDecimal.ZERO);
        }

        for (Object[] row : orderRepository.getRevenueRowsBetween(
                OrderStatus.COMPLETED, start.atStartOfDay(), end.atStartOfDay())) {
            LocalDate weekStart = toDate(row[0]).with(DayOfWeek.MONDAY);
            if (totals.containsKey(weekStart)) {
                totals.put(weekStart, totals.get(weekStart).add(toBigDecimal(row[1])));
            }
        }

        return new RevenueSeries(
                totals.keySet().stream().map(d -> d.format(DAY_LABEL)).toList(),
                new ArrayList<>(totals.values()));
    }

    private RevenueSeries buildMonthlyRevenue(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth start = currentMonth.minusMonths(11);
        LocalDateTime startDate = start.atDay(1).atStartOfDay();
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();
        LinkedHashMap<YearMonth, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            totals.put(start.plusMonths(i), BigDecimal.ZERO);
        }

        for (Object[] row : orderRepository.getRevenueRowsBetween(
                OrderStatus.COMPLETED, startDate, endDate)) {
            YearMonth month = YearMonth.from(toDate(row[0]));
            if (totals.containsKey(month)) {
                totals.put(month, totals.get(month).add(toBigDecimal(row[1])));
            }
        }

        return new RevenueSeries(
                totals.keySet().stream().map(m -> m.format(MONTH_LABEL)).toList(),
                new ArrayList<>(totals.values()));
    }

    private RevenueSeries buildYearlyRevenue(LocalDate today) {
        int startYear = today.getYear() - 4;
        LocalDateTime startDate = LocalDate.of(startYear, 1, 1).atStartOfDay();
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();
        LinkedHashMap<Integer, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            totals.put(startYear + i, BigDecimal.ZERO);
        }

        for (Object[] row : orderRepository.getRevenueRowsBetween(
                OrderStatus.COMPLETED, startDate, endDate)) {
            int year = toDate(row[0]).getYear();
            if (totals.containsKey(year)) {
                totals.put(year, totals.get(year).add(toBigDecimal(row[1])));
            }
        }

        return new RevenueSeries(
                totals.keySet().stream().map(String::valueOf).toList(),
                new ArrayList<>(totals.values()));
    }

    private static LocalDate toDate(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported date value: " + value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private record RevenueSeries(List<String> labels, List<BigDecimal> data) {
    }
}
