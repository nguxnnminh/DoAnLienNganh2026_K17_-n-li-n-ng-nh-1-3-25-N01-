package com.shop.clothingstore.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shop.clothingstore.entity.Order;

public class DashboardDTO {

    private final long totalTransactions;
    private final long ordersToday;
    private final long pendingTransactions;
    private final long processingTransactions;
    private final long shippingTransactions;
    private final long completedTransactions;
    private final long cancelledTransactions;
    private final long cancelRequestedTransactions;
    private final BigDecimal totalAmount;
    private final BigDecimal revenueToday;
    private final BigDecimal revenueThisWeek;
    private final BigDecimal revenueThisMonth;
    private final BigDecimal revenueThisYear;
    private final BigDecimal avgOrderValue;
    private final long totalUsers;
    private final long totalStock;
    private final long totalSold;
    private final List<Order> latestTransactions;
    private final List<String> dailyRevenueLabels;
    private final List<BigDecimal> dailyRevenueData;
    private final List<String> weeklyRevenueLabels;
    private final List<BigDecimal> weeklyRevenueData;
    private final List<String> monthlyRevenueLabels;
    private final List<BigDecimal> monthlyRevenueData;
    private final List<String> yearlyRevenueLabels;
    private final List<BigDecimal> yearlyRevenueData;

    public DashboardDTO(
            long totalTransactions,
            long ordersToday,
            long pendingTransactions,
            long processingTransactions,
            long shippingTransactions,
            long completedTransactions,
            long cancelledTransactions,
            long cancelRequestedTransactions,
            BigDecimal totalAmount,
            BigDecimal revenueToday,
            BigDecimal revenueThisWeek,
            BigDecimal revenueThisMonth,
            BigDecimal revenueThisYear,
            BigDecimal avgOrderValue,
            long totalUsers,
            long totalStock,
            long totalSold,
            List<Order> latestTransactions,
            List<String> dailyRevenueLabels,
            List<BigDecimal> dailyRevenueData,
            List<String> weeklyRevenueLabels,
            List<BigDecimal> weeklyRevenueData,
            List<String> monthlyRevenueLabels,
            List<BigDecimal> monthlyRevenueData,
            List<String> yearlyRevenueLabels,
            List<BigDecimal> yearlyRevenueData) {
        this.totalTransactions = totalTransactions;
        this.ordersToday = ordersToday;
        this.pendingTransactions = pendingTransactions;
        this.processingTransactions = processingTransactions;
        this.shippingTransactions = shippingTransactions;
        this.completedTransactions = completedTransactions;
        this.cancelledTransactions = cancelledTransactions;
        this.cancelRequestedTransactions = cancelRequestedTransactions;
        this.totalAmount = safe(totalAmount);
        this.revenueToday = safe(revenueToday);
        this.revenueThisWeek = safe(revenueThisWeek);
        this.revenueThisMonth = safe(revenueThisMonth);
        this.revenueThisYear = safe(revenueThisYear);
        this.avgOrderValue = safe(avgOrderValue);
        this.totalUsers = totalUsers;
        this.totalStock = totalStock;
        this.totalSold = totalSold;
        this.latestTransactions = latestTransactions;
        this.dailyRevenueLabels = dailyRevenueLabels;
        this.dailyRevenueData = dailyRevenueData;
        this.weeklyRevenueLabels = weeklyRevenueLabels;
        this.weeklyRevenueData = weeklyRevenueData;
        this.monthlyRevenueLabels = monthlyRevenueLabels;
        this.monthlyRevenueData = monthlyRevenueData;
        this.yearlyRevenueLabels = yearlyRevenueLabels;
        this.yearlyRevenueData = yearlyRevenueData;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getOrdersToday() {
        return ordersToday;
    }

    public long getPendingTransactions() {
        return pendingTransactions;
    }

    public long getProcessingTransactions() {
        return processingTransactions;
    }

    public long getShippingTransactions() {
        return shippingTransactions;
    }

    public long getCompletedTransactions() {
        return completedTransactions;
    }

    public long getCancelledTransactions() {
        return cancelledTransactions;
    }

    public long getCancelRequestedTransactions() {
        return cancelRequestedTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getRevenueToday() {
        return revenueToday;
    }

    public BigDecimal getRevenueThisWeek() {
        return revenueThisWeek;
    }

    public BigDecimal getRevenueThisMonth() {
        return revenueThisMonth;
    }

    public BigDecimal getRevenueThisYear() {
        return revenueThisYear;
    }

    public BigDecimal getAvgOrderValue() {
        return avgOrderValue;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalStock() {
        return totalStock;
    }

    public long getTotalSold() {
        return totalSold;
    }

    public List<Order> getLatestTransactions() {
        return latestTransactions;
    }

    public List<String> getDailyRevenueLabels() {
        return dailyRevenueLabels;
    }

    public List<BigDecimal> getDailyRevenueData() {
        return dailyRevenueData;
    }

    public List<String> getWeeklyRevenueLabels() {
        return weeklyRevenueLabels;
    }

    public List<BigDecimal> getWeeklyRevenueData() {
        return weeklyRevenueData;
    }

    public List<String> getMonthlyRevenueLabels() {
        return monthlyRevenueLabels;
    }

    public List<BigDecimal> getMonthlyRevenueData() {
        return monthlyRevenueData;
    }

    public List<String> getYearlyRevenueLabels() {
        return yearlyRevenueLabels;
    }

    public List<BigDecimal> getYearlyRevenueData() {
        return yearlyRevenueData;
    }

    public List<String> getAmountLabels() {
        return dailyRevenueLabels;
    }

    public List<BigDecimal> getAmountData() {
        return dailyRevenueData;
    }
}
