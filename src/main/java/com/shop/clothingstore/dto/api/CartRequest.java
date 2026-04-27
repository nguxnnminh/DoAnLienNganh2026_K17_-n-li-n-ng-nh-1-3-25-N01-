package com.shop.clothingstore.dto.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartRequest {

    @NotNull(message = "variantId không được null")
    private Long variantId;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Max(value = 99, message = "Số lượng tối đa là 99")
    private int quantity = 1;
}
