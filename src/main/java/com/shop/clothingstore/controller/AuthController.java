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

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String email,
            @RequestParam String password
    ) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        userRepository.save(user);
        return "redirect:/login";
    }
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email) {

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/forgot-password";
        }

        User user = userOpt.get();

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));

        tokenRepository.save(token);

        // DEMO: in link ra console
        System.out.println("RESET LINK: http://localhost:8080/reset-password?token=" + token.getToken());

        return "redirect:/login";
    }
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {

        Optional<PasswordResetToken> tokenOpt =
                tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                    @RequestParam String password) {

        Optional<PasswordResetToken> tokenOpt =
                tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login";
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        tokenRepository.delete(resetToken);

        return "redirect:/login";
    }
}
