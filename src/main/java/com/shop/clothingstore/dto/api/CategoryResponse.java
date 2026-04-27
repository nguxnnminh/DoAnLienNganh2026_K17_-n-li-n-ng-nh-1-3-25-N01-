package com.shop.clothingstore.dto.api;

import com.shop.clothingstore.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug()
        );
    }
}
