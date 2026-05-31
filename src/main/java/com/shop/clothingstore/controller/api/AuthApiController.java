package com.shop.clothingstore.controller.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.AuthResponse;
import com.shop.clothingstore.dto.api.LoginRequest;
import com.shop.clothingstore.dto.api.RegisterRequest;
import com.shop.clothingstore.entity.PasswordResetToken;
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.security.JwtUtil;
import com.shop.clothingstore.service.EmailService;
import com.shop.clothingstore.service.PasswordResetService;
import com.shop.clothingstore.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    private final String publicBaseUrl;

    public AuthApiController(
            AuthenticationManager authManager,
            JwtUtil jwtUtil,
            UserService userService,
            PasswordEncoder passwordEncoder,
            PasswordResetService passwordResetService,
            EmailService emailService,
            @org.springframework.beans.factory.annotation.Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {

        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
        this.publicBaseUrl = publicBaseUrl;
    }

    // =====================================================
    // POST /api/auth/register
    // =====================================================
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userService.existsByEmail(normalizedEmail)) {
            log.warn("Registration attempt with existing email: {}", normalizedEmail);
            throw new IllegalStateException("Registration failed. Please check your information.");
        }

        User user = userService.registerUser(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                Role.USER,
                request.getRef(),
                request.getFullName());

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("User registered via API | email={}", user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }

    // =====================================================
    // POST /api/auth/login
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("User login via API | email={}", user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }

    // =====================================================
    // POST /api/auth/forgot-password
    // Body: { "email": "user@example.com" }
    // Always returns 200 (don't leak email existence)
    // =====================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userService.findByEmail(email).ifPresent(user -> {
            try {
                PasswordResetToken token = passwordResetService.createTokenForUser(user);
                String link = publicBaseUrl + "/reset-password?token=" + token.getToken();
                emailService.sendResetPasswordEmail(user.getEmail(), link);
                log.info("Password reset email sent via API | email={}", email);
            } catch (Exception ex) {
                log.error("Failed to send password reset email | email={}", email, ex);
            }
        });
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered, a reset link has been sent."));
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    // =====================================================
    // POST /api/auth/reset-password
    // Body: { "token": "...", "password": "...", "confirmPassword": "..." }
    // =====================================================
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
        }

        var tokenOpt = passwordResetService.findByToken(request.getToken());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired reset token"));
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reset token has expired"));
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userService.save(user);
        passwordResetService.deleteToken(resetToken);

        log.info("Password reset via API | email={}", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please sign in."));
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank @Size(min = 8)
        private String password;
        @NotBlank
        private String confirmPassword;
    }
}
