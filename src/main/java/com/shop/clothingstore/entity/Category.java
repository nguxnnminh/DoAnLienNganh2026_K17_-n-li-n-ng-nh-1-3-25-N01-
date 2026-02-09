package com.shop.clothingstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TOP, BOTTOM, ACCESSORY
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true) // Thêm slug
    private String slug; // Ví dụ: "top", "bottom", "accessories"
}
