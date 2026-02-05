package com.shop.clothingstore.dto;

import lombok.Data;

@Data
public class ProductFilterDTO {
    private Long categoryId;
    private Long subCategoryId;
    private Double minPrice;
    private Double maxPrice;
    private String keyword;
}
