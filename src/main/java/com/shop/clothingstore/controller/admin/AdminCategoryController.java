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
@RequestMapping("/admin/categories")
public class AdminCategoryController extends AdminBaseController {

    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;

    public AdminCategoryController(CategoryService categoryService,
                                   SubCategoryService subCategoryService) {
        this.categoryService = categoryService;
        this.subCategoryService = subCategoryService;
    }

    // ── LIST ────────────────────────────────────────────
    @GetMapping
    public String list(Model model) {
        model.addAttribute("title", "Categories");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories/index";
    }

    // ── CREATE FORM ─────────────────────────────────────
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("title", "Add Category");
        model.addAttribute("currentPage", "categories");
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new Category());
        }
        return "admin/categories/create";
    }

    // ── CREATE POST ──────────────────────────────────────
    @PostMapping("/create")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String slug,
                         RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Category name cannot be empty.");
            return "redirect:/admin/categories/create";
        }
        String finalSlug = (slug != null && !slug.isBlank())
                ? slug.trim().toLowerCase()
                : generateUniqueSlug(name);

        if (categoryService.getCategoryBySlug(finalSlug).isPresent()) {
            ra.addFlashAttribute("error", "Slug '" + finalSlug + "' already exists.");
            return "redirect:/admin/categories/create";
        }

        try {
            Category c = new Category();
            c.setName(name.trim());
            c.setSlug(finalSlug);
            categoryService.saveCategory(c);
            ra.addFlashAttribute("success", "Category '" + name.trim() + "' created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ── EDIT FORM ────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return categoryService.getCategoryById(id).map(c -> {
            model.addAttribute("title", "Edit Category");
            model.addAttribute("currentPage", "categories");
            model.addAttribute("category", c);
            return "admin/categories/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        });
    }

    // ── UPDATE POST ──────────────────────────────────────
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String slug,
                         RedirectAttributes ra) {
        Category existing = categoryService.getCategoryById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        }
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Category name cannot be empty.");
            return "redirect:/admin/categories/" + id + "/edit";
        }

        String finalSlug = (slug != null && !slug.isBlank())
                ? slug.trim().toLowerCase()
                : toSlug(name);

        // Check uniqueness only if slug changed
        if (!finalSlug.equals(existing.getSlug())) {
            if (categoryService.getCategoryBySlug(finalSlug).isPresent()) {
                ra.addFlashAttribute("error", "Slug '" + finalSlug + "' already exists.");
                return "redirect:/admin/categories/" + id + "/edit";
            }
        }

        try {
            existing.setName(name.trim());
            existing.setSlug(finalSlug);
            categoryService.saveCategory(existing);
            ra.addFlashAttribute("success", "Category updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ── DELETE POST ──────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        List<SubCategory> subs = subCategoryService.getByCategoryId(id);
        if (!subs.isEmpty()) {
            ra.addFlashAttribute("error",
                    "Cannot delete: category still has " + subs.size() + " subcategory(s).");
            return "redirect:/admin/categories";
        }
        try {
            categoryService.deleteCategory(id);
            ra.addFlashAttribute("success", "Category deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete category.");
        }
        return "redirect:/admin/categories";
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
        while (categoryService.getCategoryBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
