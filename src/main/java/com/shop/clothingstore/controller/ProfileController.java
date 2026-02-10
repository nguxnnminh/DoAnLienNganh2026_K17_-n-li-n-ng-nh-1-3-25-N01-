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
                                @RequestParam String address) {

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);

        userRepository.save(user);

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
                                 @RequestParam String confirmPassword) {

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();

        // Check mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return "redirect:/profile?error=oldPassword";
        }

        // Check confirm
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/profile?error=confirm";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/profile?success=password";
    }
}
