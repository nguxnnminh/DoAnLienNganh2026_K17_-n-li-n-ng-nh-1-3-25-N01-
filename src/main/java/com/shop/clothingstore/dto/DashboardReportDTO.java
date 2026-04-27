package com.shop.clothingstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardReportDTO {

    private long totalOrders;
    private long completedOrders;
    private long cancelledOrders;
    private double totalRevenue;
    private long totalCustomers;
    private double completionRate;   // %
}
