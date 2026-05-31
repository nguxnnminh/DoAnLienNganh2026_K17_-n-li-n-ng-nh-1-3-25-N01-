package com.shop.clothingstore.dto.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shop.clothingstore.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO trả về thông tin đơn hàng qua REST API. FIXED: Dùng BigDecimal cho total
 * và price — tránh floating-point imprecision khi serialize JSON.
 */
@Data
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String customerName;
    private String phone;
    private String address;
    private String status;

    // FIXED: BigDecimal thay Double — tài chính không được dùng Double
    private BigDecimal total;

    private List<OrderItemInfo> items;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    public static class OrderItemInfo {

        private String productName;
        private String size;
        private String color;

        // FIXED: BigDecimal thay Double
        private BigDecimal price;

        private Integer quantity;
    }

    /**
     * Map Order entity → OrderResponse DTO. Không expose entity trực tiếp ra
     * ngoài API.
     */
    public static OrderResponse from(Order order) {

        List<OrderItemInfo> items = order.getItems() != null
                ? order.getItems().stream()
                        .map(i -> new OrderItemInfo(
                        i.getProductName(),
                        i.getSize(),
                        i.getColor(),
                        i.getPrice(), // BigDecimal → BigDecimal, no conversion needed
                        i.getQuantity()
                ))
                        .toList()
                : List.of();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getPhone(),
                order.getAddress(),
                order.getStatus().name(),
                order.getTotal(), // BigDecimal → BigDecimal
                items,
                order.getCreatedAt()
        );
    }
}
