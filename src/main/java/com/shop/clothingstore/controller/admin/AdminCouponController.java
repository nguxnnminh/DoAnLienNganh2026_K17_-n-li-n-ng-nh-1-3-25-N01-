package com.shop.clothingstore.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.dto.CouponFormDTO;
import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.service.CouponService;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController extends AdminBaseController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ── LIST ────────────────────────────────────────────
    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       Model model) {
        model.addAttribute("title", "Coupons");
        model.addAttribute("currentPage", "coupons");

        List<Coupon> coupons = couponService.findAll();

        // Filter by search keyword
        if (search != null && !search.isBlank()) {
            String kw = search.trim().toUpperCase();
            coupons = coupons.stream()
                    .filter(c -> c.getCode().contains(kw))
                    .toList();
        }
        // Filter by status
        if ("active".equals(status)) {
            coupons = coupons.stream().filter(Coupon::isActive).toList();
        } else if ("inactive".equals(status)) {
            coupons = coupons.stream().filter(c -> !c.isActive()).toList();
        }

        model.addAttribute("coupons", coupons);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("discountTypes", Coupon.DiscountType.values());
        return "admin/coupons/index";
    }

    // ── CREATE FORM ──────────────────────────────────────
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("title", "Create Coupon");
        model.addAttribute("currentPage", "coupons");
        if (!model.containsAttribute("couponDTO")) {
            model.addAttribute("couponDTO", new CouponFormDTO());
        }
        model.addAttribute("discountTypes", Coupon.DiscountType.values());
        return "admin/coupons/create";
    }

    // ── CREATE POST ──────────────────────────────────────
    @PostMapping("/create")
    public String create(CouponFormDTO dto, RedirectAttributes ra) {
        String code = dto.getCode() == null ? "" : dto.getCode().trim().toUpperCase();
        if (code.isBlank()) {
            ra.addFlashAttribute("error", "Coupon code cannot be empty.");
            ra.addFlashAttribute("couponDTO", dto);
            return "redirect:/admin/coupons/create";
        }
        if (couponService.existsByCode(code)) {
            ra.addFlashAttribute("error", "Code '" + code + "' already exists.");
            ra.addFlashAttribute("couponDTO", dto);
            return "redirect:/admin/coupons/create";
        }
        try {
            Coupon coupon = mapFromDTO(new Coupon(), dto);
            coupon.setCode(code);
            // Admin-created coupons are public by default (visible to all users)
            coupon.setUserSpecific(false);
            couponService.save(coupon);
            ra.addFlashAttribute("success", "Coupon '" + code + "' created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            ra.addFlashAttribute("couponDTO", dto);
            return "redirect:/admin/coupons/create";
        }
        return "redirect:/admin/coupons";
    }

    // ── EDIT FORM ─────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return couponService.findById(id).map(coupon -> {
            model.addAttribute("title", "Edit Coupon");
            model.addAttribute("currentPage", "coupons");
            model.addAttribute("coupon", coupon);
            if (!model.containsAttribute("couponDTO")) {
                model.addAttribute("couponDTO", mapToDTO(coupon));
            }
            model.addAttribute("discountTypes", Coupon.DiscountType.values());
            return "admin/coupons/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Coupon code not found.");
            return "redirect:/admin/coupons";
        });
    }

    // ── UPDATE POST ──────────────────────────────────────
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, CouponFormDTO dto, RedirectAttributes ra) {
        Coupon existing = couponService.findById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Coupon code not found.");
            return "redirect:/admin/coupons";
        }
        String code = dto.getCode() == null ? "" : dto.getCode().trim().toUpperCase();
        if (code.isBlank()) {
            ra.addFlashAttribute("error", "Coupon code cannot be empty.");
            return "redirect:/admin/coupons/" + id + "/edit";
        }
        // Check code uniqueness only if changed
        if (!code.equals(existing.getCode()) && couponService.existsByCode(code)) {
            ra.addFlashAttribute("error", "Code '" + code + "' already exists.");
            return "redirect:/admin/coupons/" + id + "/edit";
        }
        try {
            existing.setCode(code);
            mapFromDTO(existing, dto);
            couponService.save(existing);
            ra.addFlashAttribute("success", "Coupon updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    // ── TOGGLE ACTIVE ────────────────────────────────────
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        couponService.findById(id).ifPresentOrElse(coupon -> {
            coupon.setActive(!coupon.isActive());
            couponService.save(coupon);
            ra.addFlashAttribute("success",
                    coupon.isActive() ? "Coupon activated." : "Coupon deactivated.");
        }, () -> ra.addFlashAttribute("error", "Coupon code not found."));
        return "redirect:/admin/coupons";
    }

    // ── DELETE POST ──────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            couponService.delete(id);
            ra.addFlashAttribute("success", "Coupon deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete coupon.");
        }
        return "redirect:/admin/coupons";
    }

    // ── MAPPING HELPERS ──────────────────────────────────
    private Coupon mapFromDTO(Coupon coupon, CouponFormDTO dto) {
        coupon.setDescription(dto.getDescription());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue() != null
                ? dto.getDiscountValue() : BigDecimal.ZERO);
        coupon.setMinOrderAmount(dto.getMinOrderAmount());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setActive(dto.isActive());

        // Parse startDate
        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            try {
                coupon.setStartDate(LocalDateTime.parse(dto.getStartDate()));
            } catch (Exception ignored) {
                coupon.setStartDate(null);
            }
        } else {
            coupon.setStartDate(null);
        }

        // Parse expiryDate
        if (dto.getExpiryDate() != null && !dto.getExpiryDate().isBlank()) {
            try {
                coupon.setExpiryDate(LocalDateTime.parse(dto.getExpiryDate()));
            } catch (Exception ignored) {
                coupon.setExpiryDate(null);
            }
        } else {
            coupon.setExpiryDate(null);
        }
        return coupon;
    }

    private CouponFormDTO mapToDTO(Coupon coupon) {
        CouponFormDTO dto = new CouponFormDTO();
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMinOrderAmount(coupon.getMinOrderAmount());
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setActive(coupon.isActive());
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        if (coupon.getStartDate() != null) {
            dto.setStartDate(coupon.getStartDate().format(fmt));
        }
        if (coupon.getExpiryDate() != null) {
            dto.setExpiryDate(coupon.getExpiryDate().format(fmt));
        }
        return dto;
    }
}
