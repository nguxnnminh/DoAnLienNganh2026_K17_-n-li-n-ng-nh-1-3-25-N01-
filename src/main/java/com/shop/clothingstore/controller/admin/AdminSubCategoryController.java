package com.shop.clothingstore.controller.admin;

import java.text.Normalizer;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.SubCategoryService;

@Controller
@RequestMapping("/admin/subcategories")
public class AdminSubCategoryController extends AdminBaseController {

    private final SubCategoryService subCategoryService;
    private final CategoryService categoryService;

    public AdminSubCategoryController(SubCategoryService subCategoryService,
                                      CategoryService categoryService) {
        this.subCategoryService = subCategoryService;
        this.categoryService = categoryService;
    }

    // ── LIST (optionally filtered by categoryId) ─────────
    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       Model model) {
        model.addAttribute("title", "Danh mục phụ");
        model.addAttribute("currentPage", "categories");
        List<SubCategory> subs = (categoryId != null)
                ? subCategoryService.getByCategoryId(categoryId)
                : subCategoryService.getAllSubCategories();
        model.addAttribute("subCategories", subs);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("filterCategoryId", categoryId);
        return "admin/subcategories/index";
    }

    // ── CREATE FORM ──────────────────────────────────────
    @GetMapping("/create")
    public String createForm(@RequestParam(required = false) Long categoryId,
                             Model model) {
        model.addAttribute("title", "Thêm danh mục phụ");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("preselectedCategoryId", categoryId);
        model.addAttribute("sizeTypes", com.shop.clothingstore.entity.SizeType.values());
        return "admin/subcategories/create";
    }

    // ── CREATE POST ──────────────────────────────────────
    @PostMapping("/create")
    public String create(@RequestParam String name,
                         @RequestParam Long categoryId,
                         @RequestParam(required = false) String sizeType,
                         @RequestParam(required = false) String slug,
                         RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Tên danh mục phụ không được để trống.");
            return "redirect:/admin/subcategories/create";
        }
        Category category = categoryService.getCategoryById(categoryId).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Danh mục chính không hợp lệ.");
            return "redirect:/admin/subcategories/create";
        }

        String finalSlug = (slug != null && !slug.isBlank())
                ? slug.trim().toLowerCase()
                : generateUniqueSlug(name);

        if (subCategoryService.getBySlug(finalSlug).isPresent()) {
            ra.addFlashAttribute("error", "Slug '" + finalSlug + "' đã tồn tại.");
            return "redirect:/admin/subcategories/create";
        }

        try {
            SubCategory sc = new SubCategory();
            sc.setName(name.trim());
            sc.setCategory(category);
            sc.setSlug(finalSlug);
            if (sizeType != null && !sizeType.isBlank()) {
                sc.setSizeType(com.shop.clothingstore.entity.SizeType.valueOf(sizeType));
            }
            subCategoryService.saveSubCategory(sc);
            ra.addFlashAttribute("success", "Tạo danh mục phụ '" + name.trim() + "' thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/subcategories";
    }

    // ── EDIT FORM ────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return subCategoryService.getSubCategoryById(id).map(sc -> {
            model.addAttribute("title", "Sửa danh mục phụ");
            model.addAttribute("currentPage", "categories");
            model.addAttribute("subCategory", sc);
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("sizeTypes", com.shop.clothingstore.entity.SizeType.values());
            return "admin/subcategories/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục phụ.");
            return "redirect:/admin/subcategories";
        });
    }

    // ── UPDATE POST ──────────────────────────────────────
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam Long categoryId,
                         @RequestParam(required = false) String sizeType,
                         @RequestParam(required = false) String slug,
                         RedirectAttributes ra) {
        SubCategory existing = subCategoryService.getSubCategoryById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục phụ.");
            return "redirect:/admin/subcategories";
        }
        Category category = categoryService.getCategoryById(categoryId).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Danh mục chính không hợp lệ.");
            return "redirect:/admin/subcategories/" + id + "/edit";
        }

        String finalSlug = (slug != null && !slug.isBlank())
                ? slug.trim().toLowerCase()
                : toSlug(name);

        if (!finalSlug.equals(existing.getSlug())
                && subCategoryService.getBySlug(finalSlug).isPresent()) {
            ra.addFlashAttribute("error", "Slug '" + finalSlug + "' đã tồn tại.");
            return "redirect:/admin/subcategories/" + id + "/edit";
        }

        try {
            existing.setName(name.trim());
            existing.setCategory(category);
            existing.setSlug(finalSlug);
            existing.setSizeType(
                    (sizeType != null && !sizeType.isBlank())
                            ? com.shop.clothingstore.entity.SizeType.valueOf(sizeType)
                            : null);
            subCategoryService.saveSubCategory(existing);
            ra.addFlashAttribute("success", "Cập nhật danh mục phụ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/subcategories";
    }

    // ── DELETE POST ──────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            subCategoryService.deleteSubCategory(id);
            ra.addFlashAttribute("success", "Đã xóa danh mục phụ.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: danh mục phụ còn sản phẩm liên kết.");
        }
        return "redirect:/admin/subcategories";
    }

    // ── SLUG HELPERS ─────────────────────────────────────
    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String s = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        return s;
    }

    private String generateUniqueSlug(String name) {
        String base = toSlug(name);
        String slug = base;
        int i = 1;
        while (subCategoryService.getBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
