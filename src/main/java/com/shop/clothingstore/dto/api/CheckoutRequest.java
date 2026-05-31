package com.shop.clothingstore.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(0|\\+84)(\\d{9}|\\d{10})$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Address is too long")
    private String address;

    private String couponCode;

    @Size(max = 500, message = "Note is too long")
    private String note;
}
