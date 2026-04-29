package com.shop.clothingstore.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductFilterDTO {

    private Long categoryId;
    private Long subCategoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;
}
