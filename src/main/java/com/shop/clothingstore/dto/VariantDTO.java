package com.shop.clothingstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VariantDTO {
    @NotBlank
    private String size;

    @NotBlank
    private String color;

    @NotNull
    @Positive
    private Double price;

    @NotNull
    @Positive
    private Integer stock;
}