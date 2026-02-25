package com.shop.clothingstore.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.dto.ProductCreateDTO;
import com.shop.clothingstore.dto.ProductUpdateDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.ProductService;
import com.shop.clothingstore.service.SubCategoryService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController extends AdminBaseController {

    private final ProductService productService;
    private final SubCategoryService subCategoryService;
    private final CategoryService categoryService;

    public AdminProductController(
            ProductService productService,
            SubCategoryService subCategoryService,
            CategoryService categoryService) {

        this.productService = productService;
        this.subCategoryService = subCategoryService;
        this.categoryService = categoryService;
    }

    // ===============================
    // LIST PRODUCTS
    // ===============================
    @GetMapping
    public String listProducts(Model model) {

        model.addAttribute("title", "Quản lý sản phẩm");

        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);

        return "admin/products/index";
    }

    // ===============================
    // SHOW CREATE FORM
    // ===============================
    @GetMapping("/create")
    public String showCreateForm(Model model) {

        model.addAttribute("title", "Thêm sản phẩm");

        if (!model.containsAttribute("productDTO")) {
            model.addAttribute("productDTO", new ProductCreateDTO());
        }

        model.addAttribute("categories", categoryService.getAllCategories());

        return "admin/products/create";
    }

    // ===============================
    // CREATE PRODUCT
    // ===============================
    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute("productDTO") ProductCreateDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.productDTO",
                    result);

            redirectAttributes.addFlashAttribute("productDTO", dto);

            return "redirect:/admin/products/create";
        }

        try {
            productService.createProduct(dto);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Tạo sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi khi upload ảnh.");
            return "redirect:/admin/products/create";
        }

        return "redirect:/admin/products";
    }

    // ===============================
    // DELETE PRODUCT
    // ===============================
    @DeleteMapping("/{id}")
    public String deleteProduct(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xóa sản phẩm.");
        }

        return "redirect:/admin/products";
    }

    // ===============================
    // SHOW EDIT FORM
    // ===============================
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            model.addAttribute("title", "Chỉnh sửa sản phẩm");

            Product product = productService.getProductForEdit(id);

            if (!model.containsAttribute("productDTO")) {

                ProductUpdateDTO dto = new ProductUpdateDTO();
                dto.setId(product.getId());
                dto.setName(product.getName());
                dto.setDescription(product.getDescription());
                dto.setSubCategoryId(product.getSubCategory().getId());
                dto.setActive(product.isActive());

                List<VariantDTO> variants = new ArrayList<>();
                for (ProductVariant v : product.getProductVariants()) {
                    VariantDTO vd = new VariantDTO();
                    vd.setSize(v.getSize());
                    vd.setColor(v.getColor());
                    vd.setPrice(v.getPrice());
                    vd.setStock(v.getStock());
                    variants.add(vd);
                }

                dto.setVariants(variants);

                model.addAttribute("productDTO", dto);
            }

            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("subCategories", subCategoryService.getAllSubCategories());
            model.addAttribute("existingImages", product.getImages());
            model.addAttribute("productId", id);
            model.addAttribute("selectedCategoryId",
                    product.getSubCategory().getCategory().getId());

            return "admin/products/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không tìm thấy sản phẩm.");
            return "redirect:/admin/products";
        }
    }

    // ===============================
    // UPDATE PRODUCT
    // ===============================
    @PostMapping("/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("productDTO") ProductUpdateDTO dto,
            BindingResult result,
            @RequestParam(value = "imagesToDelete", required = false) List<Long> imagesToDelete,
            RedirectAttributes redirectAttributes) {

        dto.setId(id);
        dto.setImagesToDelete(imagesToDelete);

        if (result.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.productDTO",
                    result);

            redirectAttributes.addFlashAttribute("productDTO", dto);

            return "redirect:/admin/products/" + id + "/edit";
        }

        try {
            productService.updateProduct(id, dto);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi khi xử lý ảnh.");
            return "redirect:/admin/products/" + id + "/edit";
        }

        return "redirect:/admin/products";
    }
}
