package com.shop.clothingstore.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Tên khách hàng không được trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^(0|\\+84)(\\d{9}|\\d{10})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Địa chỉ không được trống")
    @Size(max = 500, message = "Địa chỉ quá dài")
    private String address;

    // Optional — null nếu không dùng coupon
    private String couponCode;

    @Size(max = 500, message = "Ghi chú quá dài")
    private String note;
}
