package com.shop.clothingstore.entity;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sub_categories")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    private SizeType sizeType;

    @Column(nullable = false, unique = true)
    private String slug;
}