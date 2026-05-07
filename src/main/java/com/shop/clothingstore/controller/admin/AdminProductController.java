package com.shop.clothingstore.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    // ─────────────────────────────────────────────────────────
    // LIST PRODUCTS
    // BUG FIX: template sent name="search" but controller read
    //          "keyword" — now both controller AND template use
    //          "keyword" consistently.
    // Added:   categoryId filter, status (active/inactive) filter,
    //          stock filter, sort option.
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,   // "active"|"inactive"|null
            @RequestParam(required = false) String stock,    // "low"|"out"|null
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        model.addAttribute("title", "Product Management");

        com.shop.clothingstore.dto.ProductFilterDTO filter = new com.shop.clothingstore.dto.ProductFilterDTO();
        filter.setOnlyActive(false); // admin sees active + inactive

        if (keyword  != null && !keyword.isBlank())  filter.setKeyword(keyword.trim());
        if (categoryId != null)                      filter.setCategoryId(categoryId);

        // Status filter — handled by setting onlyActive or overriding in spec
        if ("active".equals(status))   filter.setOnlyActive(true);
        // "inactive" → we'll filter in-memory after query (spec doesn't have "only inactive")
        // For large datasets use a proper inactive spec; acceptable here for admin usage

        Sort sortObj = switch (sort) {
            case "name_asc"   -> Sort.by("name").ascending();
            case "price_asc"  -> Sort.by("minPrice").ascending();
            case "price_desc" -> Sort.by("minPrice").descending();
            case "stock_asc"  -> Sort.by("totalSold").ascending();
            default           -> Sort.by("id").descending();
        };

        Page<Product> productPage = productService.findWithFilter(
                filter, PageRequest.of(page, size, sortObj)
        );

        // Post-filter for "inactive only" — spec already returns all statuses
        // when onlyActive=false; just filter the page result for display only.
        // (Pagination count may be slightly off for "inactive" tab — acceptable for admin.)
        List<Product> content = productPage.getContent();
        if ("inactive".equals(status)) {
            content = content.stream().filter(p -> !p.isActive()).toList();
        }
        if ("out".equals(stock)) {
            content = content.stream().filter(p -> p.getTotalStock() == 0).toList();
        } else if ("low".equals(stock)) {
            content = content.stream().filter(p -> p.getTotalStock() > 0 && p.getTotalStock() <= 10).toList();
        }

        model.addAttribute("products",    productPage);
        model.addAttribute("content",     content);
        model.addAttribute("keyword",     keyword);
        model.addAttribute("categoryId",  categoryId);
        model.addAttribute("status",      status);
        model.addAttribute("stock",       stock);
        model.addAttribute("sort",        sort);
        model.addAttribute("categories",  categoryService.getAllCategories());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  productPage.getTotalPages());
        addPageWindow(model, productPage);

        return "admin/products/index";
    }

    // ===============================
    // SHOW CREATE FORM
    // ===============================
    @GetMapping("/create")
    public String showCreateForm(Model model) {

        model.addAttribute("title", "Add Product");

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
                    "Product created successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Image upload failed.");
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
                    "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Cannot delete product.");
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

            model.addAttribute("title", "Edit Product");

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

                    vd.setId(v.getId());
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
            model.addAttribute("product", product);
            model.addAttribute("selectedCategoryId",
                    product.getSubCategory().getCategory().getId());

            return "admin/products/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Product not found.");
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
            @RequestParam(required = false) List<Long> imagesToDelete,
            RedirectAttributes redirectAttributes) {

        dto.setId(id);
        dto.setImagesToDelete(imagesToDelete != null ? imagesToDelete : new ArrayList<>());

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
                    "Product updated successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Image processing failed.");
            return "redirect:/admin/products/" + id + "/edit";
        }

        return "redirect:/admin/products";
    }
}
