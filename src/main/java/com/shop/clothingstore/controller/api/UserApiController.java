package com.shop.clothingstore.controller.api;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.ChangePasswordRequest;
import com.shop.clothingstore.dto.api.ProfileResponse;
import com.shop.clothingstore.dto.api.ProfileUpdateRequest;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class UserApiController {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+84|0)(3|5|7|8|9)\\d{8}$");

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserApiController(UserService userService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // GET /api/profile
    // =====================================================
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(ProfileResponse.from(user));
    }

    // =====================================================
    // PUT /api/profile
    // =====================================================
    @PutMapping
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Principal principal) {

        User user = getUser(principal);

        String fullName = request.getFullName();
        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Full name cannot be empty"));
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank()
                && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid phone number format"));
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        user.setAddress(request.getAddress() != null
                ? request.getAddress().trim()
                : null);

        userService.save(user);
        return ResponseEntity.ok(ProfileResponse.from(user));
    }

    // =====================================================
    // POST /api/profile/change-password
    // =====================================================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {

        User user = getUser(principal);

        if (!passwordEncoder.matches(request.getOldPassword(),
                user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Incorrect current password"));
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Passwords do not match"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User getUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required");
        }
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
