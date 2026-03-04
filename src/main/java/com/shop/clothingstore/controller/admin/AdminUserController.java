package com.shop.clothingstore.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController extends AdminBaseController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // ===============================
    // LIST USERS
    // ===============================
    @GetMapping
    public String listUsers(Model model) {

        model.addAttribute("title", "Quản lý khách hàng");

        model.addAttribute("users", userService.findAll()); // 🔥 dùng generic

        return "admin/users/index";
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

            User user = userService.findById(id) // 🔥 dùng generic
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("title", "Chỉnh sửa khách hàng");
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());

            return "admin/users/edit";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không tìm thấy người dùng.");

            return "redirect:/admin/users";
        }
    }

    // ===============================
    // UPDATE USER
    // ===============================
    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute User updatedUser,
            RedirectAttributes redirectAttributes) {

        try {

            userService.updateUser(id, updatedUser);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật thông tin người dùng thành công!");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Cập nhật thất bại.");
        }

        return "redirect:/admin/users";
    }

    // ===============================
    // DELETE USER
    // ===============================
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            userService.deleteUser(id);  // vẫn dùng custom logic

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Xóa người dùng thành công!");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xóa người dùng.");
        }

        return "redirect:/admin/users";
    }
}
