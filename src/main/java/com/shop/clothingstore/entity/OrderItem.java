package com.shop.clothingstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Snapshot dữ liệu
    private String productName;
    private String size;
    private String color;
    private double price;
    private int quantity;

    // ✅ Thêm trường này để quản lý stock chính xác
    private Long variantId;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
