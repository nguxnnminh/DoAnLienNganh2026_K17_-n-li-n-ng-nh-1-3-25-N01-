package com.shop.clothingstore.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sub_categories")
@Data
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tee, Hoodie, Shoes, Bag...
    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    private SizeType sizeType;
}
