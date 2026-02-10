package com.shop.clothingstore.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.clothingstore.entity.PasswordResetToken;
import com.shop.clothingstore.entity.User;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    // ===== Tìm token =====
    Optional<PasswordResetToken> findByToken(String token);

    // ===== Xóa token theo user =====
    void deleteByUser(User user);

    // ===== Kiểm tra token của user =====
    Optional<PasswordResetToken> findByUser(User user);

    // ===== Cleanup token hết hạn (production useful) =====
    void deleteByExpiryDateBefore(LocalDateTime time);
}
