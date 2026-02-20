package com.shop.clothingstore.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ProductUpdateDTO {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    private String name;
    private String description;
    private Long subCategoryId;
    private Boolean active = true;

    private List<VariantDTO> variants = new ArrayList<>();
    private List<MultipartFile> newImages = new ArrayList<>();
    private List<Long> imagesToDelete = new ArrayList<>();
    private Integer primaryImageIndex = 0;
}