package com.shop.clothingstore.entity;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

    // Snapshot dữ liệu
    private String productName;
    private String size;
    private String color;

    private Double price;
    private Integer quantity;

    // Quản lý stock chính xác
    private Long variantId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}