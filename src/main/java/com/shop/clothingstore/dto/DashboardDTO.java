package com.shop.clothingstore.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shop.clothingstore.entity.Order;

public class DashboardDTO {

    private long totalTransactions;
    private long pendingTransactions;
    private long completedTransactions;
    private BigDecimal totalAmount;
    private long totalUsers;

    private List<Order> latestTransactions;

    private List<String> amountLabels;
    private List<BigDecimal> amountData;

    public DashboardDTO(
            long totalTransactions,
            long pendingTransactions,
            long completedTransactions,
            BigDecimal totalAmount,
            long totalUsers,
            List<Order> latestTransactions,
            List<String> amountLabels,
            List<BigDecimal> amountData) {

        this.totalTransactions = totalTransactions;
        this.pendingTransactions = pendingTransactions;
        this.completedTransactions = completedTransactions;
        this.totalAmount = totalAmount;
        this.totalUsers = totalUsers;
        this.latestTransactions = latestTransactions;
        this.amountLabels = amountLabels;
        this.amountData = amountData;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getPendingTransactions() {
        return pendingTransactions;
    }

    public long getCompletedTransactions() {
        return completedTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public List<Order> getLatestTransactions() {
        return latestTransactions;
    }

    public List<String> getAmountLabels() {
        return amountLabels;
    }

    public List<BigDecimal> getAmountData() {
        return amountData;
    }
}
