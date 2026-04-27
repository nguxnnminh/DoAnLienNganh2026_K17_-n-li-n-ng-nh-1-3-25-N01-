package com.shop.clothingstore.dto.api;

import com.shop.clothingstore.entity.SubCategory;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubCategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;

    public static SubCategoryResponse from(SubCategory sc) {
        return new SubCategoryResponse(
                sc.getId(),
                sc.getName(),
                sc.getSlug(),
                sc.getCategory().getId(),
                sc.getCategory().getName()
        );
    }
}
