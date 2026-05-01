package com.shop.clothingstore.controller.admin;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.GarmentType;
import com.shop.clothingstore.service.TryOnService;

/**
 * Admin controller for managing virtual try-on settings per product.
 * The try-on form is placed OUTSIDE the product edit form to avoid
 * the nested-form HTML bug (see conversation e0b38546).
 */
@Controller
@RequestMapping("/admin/products")
public class AdminTryOnController extends AdminBaseController {

    private final TryOnService tryOnService;

    public AdminTryOnController(TryOnService tryOnService) {
        this.tryOnService = tryOnService;
    }

    /**
     * Preprocess a garment image and enable try-on for a product.
     */
    @PostMapping("/{id}/tryon/enable")
    public String enableTryOn(
            @PathVariable Long id,
            @RequestParam("garmentImage") MultipartFile garmentImage,
            @RequestParam(value = "garmentType", defaultValue = "UPPER_BODY") GarmentType garmentType,
            RedirectAttributes redirectAttributes) {

        if (garmentImage.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ảnh garment để upload.");
            return "redirect:/admin/products/" + id + "/edit";
        }

        try {
            tryOnService.preprocessAndEnable(id, garmentImage, garmentType);
            redirectAttributes.addFlashAttribute("success",
                    "Đã bật Try-On và lưu garment image cho sản phẩm.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi upload garment image: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/products/" + id + "/edit";
    }

    /**
     * Disable try-on for a product.
     */
    @PostMapping("/{id}/tryon/disable")
    public String disableTryOn(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            tryOnService.disableTryOn(id);
            redirectAttributes.addFlashAttribute("success", "Đã tắt Try-On cho sản phẩm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/products/" + id + "/edit";
    }
}
