package com.shop.clothingstore.entity;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "product_images")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductImage extends BaseEntity {

    @Column(nullable = false)
    private String imageUrl;

    private boolean primaryImage = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;
}