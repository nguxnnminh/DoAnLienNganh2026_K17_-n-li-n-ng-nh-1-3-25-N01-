package com.shop.clothingstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "product_variants")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariant extends BaseEntity {

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer sold = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;
}