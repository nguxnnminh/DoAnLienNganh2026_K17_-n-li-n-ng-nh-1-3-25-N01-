package com.shop.clothingstore.entity;

import jakarta.persistence.*;
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
}
