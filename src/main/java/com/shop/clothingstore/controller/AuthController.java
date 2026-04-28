package com.shop.clothingstore.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.PasswordResetToken;
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.EmailService;
import com.shop.clothingstore.service.PasswordResetService;
import com.shop.clothingstore.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    public AuthController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            PasswordResetService passwordResetService,
            EmailService emailService) {

        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
    }

    // ================= LOGIN =================
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ================= REGISTER =================
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {

        // ===== normalize email =====
        String normalizedEmail = email.toLowerCase().trim();

        // ===== check email tồn tại =====
        if (userService.existsByEmail(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại");
            return "redirect:/register";
        }

        // ===== check confirm password =====
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/register";
        }

        // ===== validate password basic =====
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải tối thiểu 6 ký tự");
            return "redirect:/register";
        }

        // ===== create user =====
        userService.registerUser(
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.USER
        );

        // ===== SUCCESS TOAST =====
        redirectAttributes.addFlashAttribute("success",
                "Đăng ký thành công! Vui lòng đăng nhập.");

        return "redirect:/login";
    }

    // ================= FORGOT PASSWORD PAGE =================
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String email,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {

        // Normalize email — same as registration
        String normalizedEmail = email.toLowerCase().trim();

        Optional<User> userOpt = userService.findByEmail(normalizedEmail);

        redirectAttributes.addFlashAttribute(
                "success",
                "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi."
        );

        if (userOpt.isPresent()) {

            User user = userOpt.get();

            PasswordResetToken token
                    = passwordResetService.createTokenForUser(user);

            // Build base URL dynamically — không hardcode localhost
            String baseUrl = request.getScheme() + "://"
                    + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort() : "");

            String resetLink = baseUrl + "/reset-password?token=" + token.getToken();

            emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        }

        return "redirect:/forgot-password";
    }
    // ================= RESET PASSWORD =================

    @GetMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        Optional<PasswordResetToken> tokenOpt
                = passwordResetService.findByToken(token);

        if (tokenOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Link không hợp lệ");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Link đã hết hạn");
            return "redirect:/login";
        }

        // 👇 QUAN TRỌNG NHẤT
        model.addAttribute("token", token);

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {

        Optional<PasswordResetToken> tokenOpt
                = passwordResetService.findByToken(token);

        if (tokenOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Link không hợp lệ");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Link đã hết hạn");
            return "redirect:/login";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải tối thiểu 6 ký tự");
            return "redirect:/reset-password?token=" + token;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));

        userService.save(user);
        passwordResetService.deleteToken(resetToken);

        redirectAttributes.addFlashAttribute("success", "Đặt lại mật khẩu thành công");

        return "redirect:/login";
    }
}
