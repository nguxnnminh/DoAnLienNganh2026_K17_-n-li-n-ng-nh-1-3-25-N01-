package com.shop.clothingstore.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController extends AdminBaseController {

    private static final int PAGE_SIZE = 20;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────
    // LIST — DB-level search + role filter + pagination
    // BUG FIX: previously called findAll() — loaded ALL users
    //          with no pagination and ignored search/role params.
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            Model model) {

        model.addAttribute("title", "User Management");

        Page<User> users = userService.searchUsers(
                keyword, role,
                PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending())
        );

        model.addAttribute("users",   users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role",    role);
        model.addAttribute("roles",   Role.values());
        addPageWindow(model, users);

        return "admin/users/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("title", "Add User");
        if (!model.containsAttribute("user")) {
            User user = new User();
            user.setRole(Role.USER);
            model.addAttribute("user", user);
        }
        model.addAttribute("roles", Role.values());
        return "admin/users/create";
    }

    @PostMapping("/create")
    public String createUser(
            @ModelAttribute("user") User user,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email is required.");
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/create";
        }
        if (password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/create";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/create";
        }

        try {
            userService.createAdminManagedUser(
                    user.getEmail(),
                    passwordEncoder.encode(password),
                    user.getRole(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getAddress());
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot create user.");
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/create";
        }
    }

    // ─────────────────────────────────────────────────────────
    // EDIT FORM
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("title", "Edit User");
            model.addAttribute("user",  user);
            model.addAttribute("roles", Role.values());

            return "admin/users/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute User updatedUser,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updateUser(id, updatedUser);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // ─────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete user.");
        }

        return "redirect:/admin/users";
    }
}
