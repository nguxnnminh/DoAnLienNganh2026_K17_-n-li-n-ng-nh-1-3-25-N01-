package com.shop.clothingstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductCreateDTO {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Vui lòng chọn danh mục phụ")
    private Long subCategoryId;

    private Boolean active = true;

    // Variants
    private List<VariantDTO> variants = new ArrayList<>();

    // Images
    private List<MultipartFile> images = new ArrayList<>();

    private Integer primaryImageIndex = 0; // index của ảnh chính
}