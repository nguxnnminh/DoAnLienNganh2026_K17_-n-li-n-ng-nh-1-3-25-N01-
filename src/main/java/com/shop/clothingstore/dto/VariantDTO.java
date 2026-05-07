package com.shop.clothingstore.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.NumberFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantDTO {

    private Long id;

    @NotBlank
    private String size;

    @NotBlank
    private String color;

    @NotNull
    @Positive
    @NumberFormat(pattern = "#")
    private BigDecimal price;

    @NotNull
    @Positive
    private Integer stock;
}
