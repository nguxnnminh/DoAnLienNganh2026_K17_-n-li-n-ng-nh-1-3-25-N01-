package com.shop.clothingstore.controller.api;

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
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.security.JwtUtil;
import com.shop.clothingstore.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthApiController(
            AuthenticationManager authManager,
            JwtUtil jwtUtil,
            UserService userService,
            PasswordEncoder passwordEncoder) {

        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // POST /api/auth/register
    // =====================================================
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã được sử dụng: " + request.getEmail());
        }

        // Tạo user object, set tất cả field rồi save 1 lần duy nhất
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        user = userService.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        log.info("User registered via API | email={}", user.getEmail());

        return ResponseEntity.ok(
                new AuthResponse(token, user.getEmail(), user.getRole().name())
        );
    }

    // =====================================================
    // POST /api/auth/login
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        log.info("User login via API | email={}", user.getEmail());

        return ResponseEntity.ok(
                new AuthResponse(token, user.getEmail(), user.getRole().name())
        );
    }
}
