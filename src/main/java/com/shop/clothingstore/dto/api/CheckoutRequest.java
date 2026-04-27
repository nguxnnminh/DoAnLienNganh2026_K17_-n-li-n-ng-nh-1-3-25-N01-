package com.shop.clothingstore.dto.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Tên khách hàng không được trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được trống")
    private String phone;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;
}
