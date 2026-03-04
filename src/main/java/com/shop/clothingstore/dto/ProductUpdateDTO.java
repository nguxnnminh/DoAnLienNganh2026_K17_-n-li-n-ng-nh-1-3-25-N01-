package com.shop.clothingstore.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ProductUpdateDTO {

    private Long id;
    private String name;
    private String description;
    private Long subCategoryId;
    private Boolean active = true;

    private List<VariantDTO> variants = new ArrayList<>();
    private List<MultipartFile> newImages = new ArrayList<>();
    private List<Long> imagesToDelete = new ArrayList<>();
    private Integer primaryImageIndex = 0;

    public ProductUpdateDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<VariantDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantDTO> variants) {
        this.variants = variants;
    }

    public List<MultipartFile> getNewImages() {
        return newImages;
    }

    public void setNewImages(List<MultipartFile> newImages) {
        this.newImages = newImages;
    }

    public List<Long> getImagesToDelete() {
        return imagesToDelete;
    }

    public void setImagesToDelete(List<Long> imagesToDelete) {
        this.imagesToDelete = imagesToDelete;
    }

    public Integer getPrimaryImageIndex() {
        return primaryImageIndex;
    }

    public void setPrimaryImageIndex(Integer primaryImageIndex) {
        this.primaryImageIndex = primaryImageIndex;
    }
}
