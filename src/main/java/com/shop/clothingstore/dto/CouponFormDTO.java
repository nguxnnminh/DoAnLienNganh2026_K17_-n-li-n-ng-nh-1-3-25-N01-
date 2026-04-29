package com.shop.clothingstore.dto;

import java.math.BigDecimal;

import com.shop.clothingstore.entity.Coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponFormDTO {

    @NotBlank(message = "Mã coupon không được để trống")
    private String code;

    @NotNull(message = "Vui lòng chọn loại giảm giá")
    private Coupon.DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;

    // datetime-local input format: "yyyy-MM-ddTHH:mm" — parsed to LocalDateTime in controller
    private String expiryDate;

    private Integer usageLimit;

    private boolean active = true;
}
