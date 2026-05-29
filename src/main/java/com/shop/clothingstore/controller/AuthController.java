package com.shop.clothingstore.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

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
    public String registerPage(@RequestParam(name = "ref", required = false) String ref, Model model) {
        // Giữ mã giới thiệu (nếu có) để form gửi kèm khi submit
        if (ref != null && !ref.isBlank()) {
            model.addAttribute("ref", ref.trim());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(name = "ref", required = false) String ref,
            RedirectAttributes redirectAttributes
    ) {

        // ===== normalize email =====
        String normalizedEmail = email.toLowerCase().trim();

        // ===== check email exists =====
        if (userService.existsByEmail(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            return "redirect:/register";
        }

        // ===== check confirm password =====
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }

        // ===== validate password basic =====
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/register";
        }

        // ===== create user (kèm mã giới thiệu nếu có) =====
        userService.registerUser(
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.USER,
                ref
        );

        // ===== send confirmation email =====
        try {
            emailService.sendRegistrationEmail(normalizedEmail);
        } catch (Exception e) {
            // Ignore email sending error during registration so user creation still succeeds
            log.warn("Registration email failed for {}", normalizedEmail, e);
        }

        // ===== SUCCESS TOAST =====
        redirectAttributes.addFlashAttribute("success",
                "Registration successful! Please log in.");

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
                "If the email exists, a password reset link has been sent."
        );

        if (userOpt.isPresent()) {

            User user = userOpt.get();

            PasswordResetToken token
                    = passwordResetService.createTokenForUser(user);

            // Use trusted config base URL (prevents host header attacks)
            String resetLink = publicBaseUrl + "/reset-password?token=" + token.getToken();

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
            redirectAttributes.addFlashAttribute("error", "Invalid link");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Link has expired");
            return "redirect:/login";
        }

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
            redirectAttributes.addFlashAttribute("error", "Invalid link");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Link has expired");
            return "redirect:/login";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/reset-password?token=" + token;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));

        userService.save(user);
        passwordResetService.deleteToken(resetToken);

        redirectAttributes.addFlashAttribute("success", "Password reset successfully");

        return "redirect:/login";
    }
}
