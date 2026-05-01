package com.shop.clothingstore.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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

    // Try-On (optional — set during creation)
    private MultipartFile garmentImage;
    private String garmentType; // UPPER_BODY, LOWER_BODY, FULL_BODY
}

