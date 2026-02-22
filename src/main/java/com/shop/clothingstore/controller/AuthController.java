package com.shop.clothingstore.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
import com.shop.clothingstore.repository.PasswordResetTokenRepository;
import com.shop.clothingstore.repository.UserRepository;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordResetTokenRepository tokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
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

        // ===== check email tồn tại =====
        if (userRepository.findByEmail(email).isPresent()) {
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
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        userRepository.save(user);

        // ===== SUCCESS TOAST =====
        redirectAttributes.addFlashAttribute("success",
                "Đăng ký thành công! Vui lòng đăng nhập.");

        return "redirect:/login";
    }

    // ================= FORGOT PASSWORD =================

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String email,
            Model model
    ) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại");
            return "auth/forgot-password";
        }

        User user = userOpt.get();

        // delete token cũ nếu có
        tokenRepository.deleteByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));

        tokenRepository.save(token);

        // DEMO: in ra console
        System.out.println("RESET LINK: http://localhost:8080/reset-password?token=" + token.getToken());

        model.addAttribute("success", "Link reset đã được gửi (xem console demo)");
        return "auth/forgot-password";
    }

    // ================= RESET PASSWORD =================

    @GetMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam String token,
            Model model
    ) {

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
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
            Model model
    ) {

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "auth/reset-password";
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
        tokenRepository.delete(resetToken);

        return "redirect:/login?resetSuccess";
    }
}
