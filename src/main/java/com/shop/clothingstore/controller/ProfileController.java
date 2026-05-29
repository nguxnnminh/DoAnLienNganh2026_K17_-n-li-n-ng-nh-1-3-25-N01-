package com.shop.clothingstore.controller;

import java.util.regex.Pattern;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+84|0)(3|5|7|8|9)\\d{8}$");

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserService userService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     ==========================
     PROFILE PAGE
     ==========================
     */
    @GetMapping
    public String profilePage(Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        model.addAttribute("user", user);

        return "shop/profile";
    }

    /*
     ==========================
     UPDATE PROFILE
     ==========================
     */
    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {

        if (fullName == null || fullName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Full name cannot be empty.");
            return "redirect:/profile";
        }
        if (fullName.length() > 100) {
            redirectAttributes.addFlashAttribute("error", "Full name must not exceed 100 characters.");
            return "redirect:/profile";
        }
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            redirectAttributes.addFlashAttribute("error", "Invalid phone number. Please use a Vietnamese number (e.g. 0901234567).");
            return "redirect:/profile";
        }
        if (address != null && address.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "Address must not exceed 500 characters.");
            return "redirect:/profile";
        }

        User user = userService
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        user.setFullName(fullName.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setAddress(address != null ? address.trim() : null);

        userService.save(user);

        redirectAttributes.addFlashAttribute("success", "Information updated successfully");

        return "redirect:/profile";
    }

    /*
     ==========================
     CHANGE PASSWORD
     ==========================
     */
    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        User user = userService
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Incorrect current password");
            return "redirect:/profile";
        }

        // Check confirm
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/profile";
        }

        // Validate length — consistent with registration requirement
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            return "redirect:/profile";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);

        redirectAttributes.addFlashAttribute("success", "Password changed successfully");

        return "redirect:/profile";
    }
}
