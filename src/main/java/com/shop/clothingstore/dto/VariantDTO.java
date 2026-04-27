package com.shop.clothingstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantDTO {

    private Long id;   // ⭐ thêm dòng này

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
