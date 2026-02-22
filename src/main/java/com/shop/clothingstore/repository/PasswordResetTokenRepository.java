package com.shop.clothingstore.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.PasswordResetToken;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface PasswordResetTokenRepository
        extends BaseRepository<PasswordResetToken, Long> {

    // ===== Tìm token =====
    Optional<PasswordResetToken> findByToken(String token);

    // ===== Xóa token theo user =====
    @Modifying
    @Transactional
    void deleteByUser(User user);

    // ===== Kiểm tra token của user =====
    Optional<PasswordResetToken> findByUser(User user);

    // ===== Cleanup token hết hạn (production useful) =====
    void deleteByExpiryDateBefore(LocalDateTime time);
}