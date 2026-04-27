package com.shop.clothingstore.dto.api;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shop.clothingstore.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String customerName;
    private String phone;
    private String address;
    private String status;
    private Double total;
    private List<OrderItemInfo> items;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    public static class OrderItemInfo {

        private String productName;
        private String size;
        private String color;
        private Double price;
        private Integer quantity;
    }

    public static OrderResponse from(Order order) {

        List<OrderItemInfo> items = order.getItems() != null
                ? order.getItems().stream()
                        .map(i -> new OrderItemInfo(
                        i.getProductName(),
                        i.getSize(),
                        i.getColor(),
                        i.getPrice(),
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
                order.getTotal(),
                items,
                order.getCreatedAt()
        );
    }
}
