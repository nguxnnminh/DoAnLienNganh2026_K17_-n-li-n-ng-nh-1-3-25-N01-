package com.shop.clothingstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardReportDTO {

    private long totalOrders;
    private long ordersToday;
    private long pendingOrders;
    private long processingOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelRequestedOrders;
    private long cancelledOrders;
    private double totalRevenue;
    private double revenueToday;
    private double revenueThisWeek;
    private double revenueThisMonth;
    private double revenueThisYear;
    private double averageOrderValue;
    private long totalCustomers;
    private double completionRate;   // %
}
