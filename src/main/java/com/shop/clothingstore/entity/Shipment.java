package com.shop.clothingstore.entity;

import java.time.LocalDateTime;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@ToString(exclude = "order")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Shipment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = 100)
    private String trackingNumber;

    @Column(length = 100)
    private String carrier;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String shippingAddress;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    private Double shippingFee;
}
