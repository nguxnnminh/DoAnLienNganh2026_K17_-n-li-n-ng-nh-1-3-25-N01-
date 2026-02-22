package com.shop.clothingstore.controller;

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
import com.shop.clothingstore.repository.UserRepository;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

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
            @RequestParam String phone,
            @RequestParam String address,
            RedirectAttributes redirectAttributes) {

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");

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

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        // Check mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
            return "redirect:/profile";
        }

        // Check confirm
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/profile";
        }

        // Check độ dài
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải tối thiểu 6 ký tự");
            return "redirect:/profile";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");

        return "redirect:/profile";
    }
}
